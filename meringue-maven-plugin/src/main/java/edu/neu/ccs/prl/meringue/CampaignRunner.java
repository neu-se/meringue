package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class CampaignRunner {
    private final Log log;
    private final Duration duration;

    public CampaignRunner(Log log, Duration duration) {
        if (log == null || duration == null) {
            throw new NullPointerException();
        }
        this.log = log;
        this.duration = duration;
    }

    public void run(CampaignConfiguration configuration, String frameworkName, Properties frameworkArguments)
            throws MojoExecutionException {
        FuzzFramework framework =
                AbstractMeringueMojo.createFramework(configuration, frameworkName, frameworkArguments);
        try {
            log.info("Running fuzzing campaign: " + configuration.getTestDescription());
            if (framework.canRestartCampaign()) {
                runWithResets(framework, duration);
            } else {
                if (ProcessUtil.waitFor(framework.startCampaign(), duration.toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new IOException("Campaign process terminated unexpectedly");
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Failed to execute fuzzing campaign", e);
        }
    }

    private void runWithResets(FuzzFramework framework, Duration duration) throws IOException, InterruptedException {
        long endTime = System.currentTimeMillis() + duration.toMillis();
        if (!ProcessUtil.waitFor(framework.startCampaign(), duration.toMillis(), TimeUnit.MILLISECONDS)) {
            return;
        }
        while (endTime > System.currentTimeMillis()) {
            long remaining = endTime - System.currentTimeMillis();
            if (!ProcessUtil.waitFor(framework.restartCampaign(), remaining, TimeUnit.MILLISECONDS)) {
                return;
            }
        }
    }
}
