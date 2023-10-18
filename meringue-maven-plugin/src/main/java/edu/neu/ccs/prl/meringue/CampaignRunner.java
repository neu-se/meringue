package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class CampaignRunner {
    private final CampaignValues values;
    private Process process = null;
    private boolean shutdown = false;

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
        while (endTime > System.currentTimeMillis()) {
            long remaining = endTime - System.currentTimeMillis();
            if (forkAndWait(framework, remaining)) {
                return;
            }
        }
    }

    private boolean forkAndWait(FuzzFramework framework, long timeout)
            throws IOException, MojoExecutionException, InterruptedException {
        synchronized (this) {
            if (shutdown) {
                return true;
            } else if (process == null) {
                process = framework.startCampaign();
            } else if (!framework.canRestartCampaign()) {
                throw new MojoExecutionException("Campaign process terminated unexpectedly");
            } else {
                process = framework.restartCampaign();
            }
        }
        return ProcessUtil.waitFor(process, timeout, TimeUnit.MILLISECONDS);
    }

    private synchronized void shutdown() {
        shutdown = true;
        if (process != null) {
            try {
                ProcessUtil.stop(process);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
