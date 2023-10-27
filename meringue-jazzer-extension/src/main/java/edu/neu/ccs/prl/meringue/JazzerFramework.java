package edu.neu.ccs.prl.meringue;

import org.apache.maven.surefire.shared.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public final class JazzerFramework implements FuzzFramework {
    private File outputDir;
    private File corpusDir;
    private File reproducerDir;
    private File workingDir;
    private File logFile;
    private File jazzerExecutable;
    private File jazzerBootstrapJar;
    private boolean quiet = false;
    private ProcessBuilder builder;

    @Override
    public void initialize(CampaignConfiguration config, Properties frameworkArguments) throws IOException {
        outputDir = config.getOutputDirectory();
        corpusDir = new File(outputDir, "corpus");
        reproducerDir = new File(outputDir, "reproducer");
        workingDir = new File(outputDir, "out");
        logFile = new File(outputDir, "jazzer.log");
        quiet = Boolean.parseBoolean(frameworkArguments.getProperty("quiet", "false"));
        File jazzerExec = getJazzerResource(outputDir, "jazzer");
        if (!jazzerExec.setExecutable(true)) {
            throw new IllegalStateException("Failed to assign executable permissions to Jazzer executable");
        }
        jazzerExecutable = jazzerExec;
        getJazzerResource(outputDir, "jazzer_standalone.jar");
        String resourcePath = File.separator + String.join(File.separator, "com", "code_intelligence",
                "jazzer", "runtime", "jazzer_bootstrap.jar");
        jazzerBootstrapJar = getJazzerResource(outputDir, resourcePath, "jazzer_bootstrap.jar");
        List<String> command = createCommand(config, frameworkArguments);
        builder = new ProcessBuilder().command(command).directory(workingDir);
        if (config.getEnvironment() != null) {
            builder.environment().clear();
            builder.environment().putAll(config.getEnvironment());
        }
        builder.environment().put("JAVA_HOME", FileUtil.javaExecToJavaHome(config.getJavaExecutable())
                .getAbsolutePath());
    }

    @Override
    public Process startCampaign() throws IOException {
        FileUtil.ensureDirectory(outputDir);
        FileUtil.ensureEmptyDirectory(corpusDir);
        FileUtil.ensureEmptyDirectory(reproducerDir);
        FileUtil.ensureEmptyDirectory(workingDir);
        FileUtil.delete(logFile);
        if (!quiet) {
            return ProcessUtil.start(builder, true);
        } else {
            return builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
                    .redirectError(ProcessBuilder.Redirect.appendTo(logFile)).start();
        }
    }

    @Override
    public File[] getCorpusFiles() {
        return corpusDir.listFiles();
    }

    @Override
    public File[] getFailureFiles() {
        return Arrays.stream(Objects.requireNonNull(workingDir.listFiles()))
                .filter(f -> f.getName().startsWith("crash-")).toArray(File[]::new);
    }

    @Override
    public Class<? extends Replayer> getReplayerClass() {
        return JazzerReplayer.class;
    }

    @Override
    public Collection<File> getRequiredClassPathElements() {
        return Arrays.asList(FileUtil.getClassPathElement(JazzerFramework.class), jazzerBootstrapJar);
    }

    @Override
    public boolean canRestartCampaign() {
        return true;
    }

    @Override
    public Process restartCampaign() throws IOException {
        FileUtil.ensureDirectory(outputDir);
        FileUtil.ensureDirectory(corpusDir);
        FileUtil.ensureDirectory(reproducerDir);
        FileUtil.ensureDirectory(workingDir);
        if (!quiet) {
            return ProcessUtil.start(builder, true);
        } else {
            return builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
                    .redirectError(ProcessBuilder.Redirect.appendTo(logFile)).start();
        }
    }

    private List<String> createCommand(CampaignConfiguration config, Properties frameworkArguments) {
        List<String> command = new LinkedList<>();
        command.add(jazzerExecutable.getAbsolutePath());
        String classPath = config.getTestClasspathJar().getAbsolutePath() + File.pathSeparator +
                FileUtil.getClassPathElement(JazzerFramework.class).getAbsolutePath();
        command.add("--cp=" + classPath);
        if (config.getTestMethodName().equals("fuzzerTestOneInput")) {
            command.add("--target_class=" + config.getTestClassName());
        } else {
            command.add("--target_class=" + JazzerTargetWrapper.class.getName());
            command.add("--target_args=" + config.getTestClassName() + " " + config.getTestMethodName());
        }
        command.add("--keep_going=" + Integer.MAX_VALUE);
        command.add("--reproducer_path=" + reproducerDir.getAbsolutePath());
        if (!config.getJavaOptions().isEmpty()) {
            command.add("--jvm_args=" + String.join(File.pathSeparator, escapeJavaOptions(config.getJavaOptions())));
        }
        String argLine = frameworkArguments.getProperty("argLine");
        if (argLine != null && !argLine.isEmpty()) {
            for (String s : argLine.split("\\s+")) {
                if (!s.isEmpty()) {
                    command.add(s);
                }
            }
        }
        command.add("-rss_limit_mb=0");
        command.add(corpusDir.getAbsolutePath());
        return command;
    }

    private static List<String> escapeJavaOptions(List<String> javaOptions) {
        return javaOptions.stream()
                .map(s -> s.replace(":", "\\:"))
                .collect(Collectors.toList());
    }

    private static File getJazzerResource(File outputDir, String resourceName) throws IOException {
        String resourcePathPrefix;
        if (SystemUtils.IS_OS_MAC) {
            resourcePathPrefix = "mac";
        } else if (SystemUtils.IS_OS_LINUX) {
            resourcePathPrefix = "linux";
        } else {
            throw new IllegalStateException("Operating system not supported");
        }
        File bin = new File(outputDir, "bin");
        FileUtil.ensureDirectory(bin);
        String resourcePath = resourcePathPrefix + File.separator + resourceName;
        return getJazzerResource(outputDir, resourcePath, resourceName);
    }

    private static File getJazzerResource(File outputDir, String resourcePath, String resourceName) throws IOException {
        File bin = new File(outputDir, "bin");
        FileUtil.ensureDirectory(bin);
        try (InputStream in = JazzerFramework.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Unable to locate Jazzer resource: " + resourcePath);
            }
            File out = new File(bin, resourceName);
            Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return out;
        }
    }
}
