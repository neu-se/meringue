package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class ReplayRunner {
    private final CampaignValues values;
    private final File input;
    private Process process = null;

    public ReplayRunner(CampaignValues values, File input) {
        this.input = input;
        if (values == null || input == null) {
            throw new NullPointerException();
        }
        this.values = values;
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public void run() throws MojoExecutionException {
        try {
            values.initialize();
            values.getLog().info("Replaying input: " + input);
            if (ProcessUtil.waitFor(createReplayLauncher().launch()) != 0) {
                throw new MojoExecutionException("Replay process terminated unexpectedly");
            }
        } catch (IOException | InterruptedException | ReflectiveOperationException e) {
            throw new MojoExecutionException("Failed to execute fuzzing replay", e);
        }
    }

    private JvmLauncher createReplayLauncher() throws MojoExecutionException, ReflectiveOperationException {
        CampaignConfiguration configuration = values.createCampaignConfiguration();
        FuzzFramework framework = values.createFrameworkBuilder()
                .build(configuration);
        List<String> options = new LinkedList<>(configuration.getJavaOptions());
        options.add(JvmLauncher.DEBUG_OPT + "5005");
        options.add("-cp");
        options.add(CampaignUtil.buildClassPath(
                FileUtil.getClassPathElement(ReplayForkMain.class),
                configuration.getTestClasspathJar(),
                createReplayFrameworkJar(framework)
        ));
        options.addAll(framework.getAnalysisJavaOptions());
        String[] arguments = new String[]{
                configuration.getTestClassName(),
                configuration.getTestMethodName(),
                framework.getReplayerClass().getName(),
                input.getAbsolutePath()
        };
        return JvmLauncher.fromMain(
                configuration.getJavaExecutable(),
                ReplayForkMain.class.getName(),
                options.toArray(new String[0]),
                true,
                arguments,
                configuration.getWorkingDirectory(),
                configuration.getEnvironment()
        );
    }

    private File createReplayFrameworkJar(FuzzFramework framework) throws MojoExecutionException {
        try {
            File jar = new File(values.getTemporaryDirectory(), "meringue-replay-framework.jar");
            FileUtil.buildManifestJar(framework.getRequiredClassPathElements(), jar);
            return jar;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create framework manifest JAR", e);
        }
    }

    private synchronized Process setProcess(Process process) {
        this.process = process;
        return this.process;
    }

    private synchronized void shutdown() {
        if (process != null) {
            try {
                ProcessUtil.stop(process);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}