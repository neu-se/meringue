package edu.neu.ccs.prl.meringue;

import com.code_intelligence.jazzer.api.Jazzer;
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
    private CampaignConfiguration config;
    private Properties options;

    @Override
    public void initialize(CampaignConfiguration config, Properties options) {
        this.outputDir = config.getOutputDir();
        this.corpusDir = new File(outputDir, "corpus");
        this.reproducerDir = new File(outputDir, "reproducer");
        this.workingDir = new File(outputDir, "results");
        this.config = config;
        this.options = options;
    }

    @Override
    public Process startCampaign() throws IOException {
        FileUtil.ensureDirectory(outputDir);
        FileUtil.createOrCleanDirectory(corpusDir);
        FileUtil.createOrCleanDirectory(reproducerDir);
        FileUtil.createOrCleanDirectory(workingDir);
        List<String> command = new LinkedList<>();
        command.add(getJazzerExecutable(outputDir).getAbsolutePath());
        command.add("--cp=" + config.getTestClassPathJar().getAbsolutePath());
        command.add("--target_class=" + FuzzTarget.class.getName());
        command.add("--keep_going=" + Integer.MAX_VALUE);
        command.add("--reproducer_path=" + reproducerDir.getAbsolutePath());
        command.add("--target_args=" + config.getTestClassName() + " " + config.getTestMethodName());
        if (!config.getJavaOptions().isEmpty()) {
            command.add("--jvm_args=" + String.join(File.pathSeparator, config.getJavaOptions()));
        }
        // TODO options
        command.add("-rss_limit_mb=0");
        command.add(corpusDir.getAbsolutePath());
        return ProcessUtil.start(new ProcessBuilder().command(command).directory(workingDir), true);
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
    public File[] getFrameworkClassPathElements() {
        return new File[]{
                FileUtil.getClassPathElement(JazzerFramework.class),
                FileUtil.getClassPathElement(com.code_intelligence.jazzer.replay.Replayer.class),
                FileUtil.getClassPathElement(Jazzer.class)
        };
    }

    private static File getJazzerExecutable(File outputDir) throws IOException {
        String name;
        if (SystemUtils.IS_OS_MAC) {
            name = "mac";
        } else if (SystemUtils.IS_OS_LINUX) {
            name = "linux";
        } else if (SystemUtils.IS_OS_WINDOWS) {
            name = "windows";
        } else {
            throw new IllegalStateException("Operating system not supported");
        }
        File jazzerExec = new File(outputDir, "jazzer.exe");
        String resourceName = name + File.separator + "jazzer.exe";
        try (InputStream in = JazzerFramework.class.getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IllegalStateException("Unable to locate Jazzer resource: " + resourceName);
            }
            Files.copy(in, jazzerExec.toPath(), StandardCopyOption.COPY_ATTRIBUTES,
                    StandardCopyOption.REPLACE_EXISTING);
            return jazzerExec;
        }
    }
}
