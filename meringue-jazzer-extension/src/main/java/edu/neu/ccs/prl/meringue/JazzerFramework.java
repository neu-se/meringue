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
    private ProcessBuilder builder;
    private boolean quiet = false;

    @Override
    public void initialize(CampaignConfiguration config, Properties frameworkArguments) throws IOException {
        outputDir = config.getOutputDir();
        corpusDir = new File(outputDir, "corpus");
        reproducerDir = new File(outputDir, "reproducer");
        workingDir = new File(outputDir, "out");
        logFile = new File(outputDir, "jazzer.log");
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
        quiet = Boolean.parseBoolean(frameworkArguments.getProperty("quiet", "false"));
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
        builder = new ProcessBuilder().command(command).directory(workingDir);
        builder.environment().put("JAVA_HOME", FileUtil.javaExecToJavaHome(config.getJavaExec()).getAbsolutePath());
    }

    @Override
    public Process startCampaign() throws IOException {
        FileUtil.ensureDirectory(outputDir);
        FileUtil.createOrCleanDirectory(corpusDir);
        FileUtil.createOrCleanDirectory(reproducerDir);
        FileUtil.createOrCleanDirectory(workingDir);
        if (logFile.exists() && !logFile.delete()) {
            throw new IOException("Failed to delete existing Jazzer log file: " + logFile);
        }
        if (!quiet) {
            return ProcessUtil.start(builder, true);
        } else {
            return builder.redirectError(logFile).redirectOutput(logFile).start();
        }
    }

    @Override
    public File[] getCorpusFiles() {
        return corpusDir.listFiles();
    }

    @Override
    public File[] getFailureFiles() {
        return Arrays.stream(Objects.requireNonNull(workingDir.listFiles()))
                     .filter(f -> f.getName().startsWith("crash-"))
                     .toArray(File[]::new);
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
            return builder.redirectError(logFile).redirectOutput(logFile).start();
        }
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
