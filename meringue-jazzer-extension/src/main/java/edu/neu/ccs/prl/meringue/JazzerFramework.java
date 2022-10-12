package edu.neu.ccs.prl.meringue;

import org.apache.maven.surefire.shared.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public final class JazzerFramework implements FuzzFramework {
    private File outputDir;
    private File corpusDir;
    private File reproducerDir;
    private File workingDir;
    private File logFile;
    private boolean quiet = false;
    private ProcessBuilder builder;

    @Override
    public void initialize(CampaignConfiguration config, Properties frameworkArguments) throws IOException {
        outputDir = config.getOutputDir();
        corpusDir = new File(outputDir, "corpus");
        reproducerDir = new File(outputDir, "reproducer");
        workingDir = new File(outputDir, "out");
        logFile = new File(outputDir, "jazzer.log");
        quiet = Boolean.parseBoolean(frameworkArguments.getProperty("quiet", "false"));
        quiet = Boolean.parseBoolean(frameworkArguments.getProperty("quiet", "false"));
        List<String> command = createCommand(config, frameworkArguments, outputDir, reproducerDir, corpusDir);
        builder = new ProcessBuilder().command(command).directory(workingDir);
        if (config.getEnvironment() != null) {
            builder.environment().clear();
            builder.environment().putAll(config.getEnvironment());
        }
        // TODO set user.dir to config.getWorkingDir().getAbsolutePath() to compensate for need to use specific working
        // directory for the fork
        builder.environment().put("JAVA_HOME", FileUtil.javaExecToJavaHome(config.getJavaExec()).getAbsolutePath());
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
        return Collections.singleton(FileUtil.getClassPathElement(JazzerFramework.class));
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

    private static List<String> createCommand(CampaignConfiguration config, Properties frameworkArguments,
                                              File outputDir, File reproducerDir, File corpusDir) throws IOException {
        List<String> command = new LinkedList<>();
        command.add(getJazzerExecutable(outputDir).getAbsolutePath());
        String classPath = config.getTestClassPathJar().getAbsolutePath() + File.pathSeparator +
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
            command.add("--jvm_args=" + String.join(File.pathSeparator, config.getJavaOptions()));
        }
        String argLine = frameworkArguments.getProperty("argLine");
        if (argLine != null && !argLine.isEmpty()) {
            for (String s : argLine.split("\\s")) {
                if (!s.isEmpty()) {
                    command.add(s);
                }
            }
        }
        command.add("-rss_limit_mb=0");
        command.add(corpusDir.getAbsolutePath());
        return command;
    }

    private static File getJazzerExecutable(File outputDir) throws IOException {
        String executableName = "jazzer";
        String resourcePathPrefix;
        if (SystemUtils.IS_OS_MAC) {
            resourcePathPrefix = "mac";
        } else if (SystemUtils.IS_OS_LINUX) {
            resourcePathPrefix = "linux";
        } else {
            throw new IllegalStateException("Operating system not supported");
        }
        String[] resourceNames = new String[]{"jazzer_agent_deploy.jar", "jazzer_api_deploy.jar", executableName};
        File bin = new File(outputDir, "bin");
        FileUtil.ensureDirectory(bin);
        File jazzerExec = new File(bin, executableName);
        for (String resourceName : resourceNames) {
            String resourcePath = resourcePathPrefix + File.separator + resourceName;
            try (InputStream in = JazzerFramework.class.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    throw new IllegalStateException("Unable to locate Jazzer resource: " + resourcePath);
                }
                File out = new File(bin, resourceName);
                Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        if (!jazzerExec.setExecutable(true)) {
            throw new IllegalStateException("Failed to assign executable permissions to Jazzer executable");
        }
        return jazzerExec;
    }
}
