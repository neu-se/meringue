package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class CampaignRunner {
    private final CampaignValues values;
    private Process process = null;

    public CampaignRunner(CampaignValues values) {
        if (values == null) {
            throw new NullPointerException();
        }
        this.values = values;
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public void run() throws MojoExecutionException {
        try {
            FileUtil.ensureEmptyDirectory(values.getOutputDirectory());
            values.initialize();
            FuzzFramework framework = values.createFrameworkBuilder()
                                            .build(values.createCampaignConfiguration());
            values.getLog().info("Running fuzzing campaign: " + values.getTestDescription());
            run(framework, values.getDuration());
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Failed to execute fuzzing campaign", e);
        }
    }

    private void run(FuzzFramework framework, Duration duration)
            throws InterruptedException, IOException, MojoExecutionException {
        long endTime = System.currentTimeMillis() + duration.toMillis();
        if (ProcessUtil.waitFor(setProcess(framework.startCampaign()), duration.toMillis(),
                                TimeUnit.MILLISECONDS)) {
            if (!framework.canRestartCampaign()) {
                throw new MojoExecutionException("Campaign process terminated unexpectedly");
            }
            while (endTime > System.currentTimeMillis()) {
                long remaining = endTime - System.currentTimeMillis();
                if (!ProcessUtil.waitFor(setProcess(framework.restartCampaign()), remaining,
                                         TimeUnit.MILLISECONDS)) {
                    return;
                }
            }
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
