package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class CampaignRunner {
    private final CampaignConfiguration configuration;
    private final Duration duration;
    private final String frameworkName;
    private final Properties frameworkArguments;

    public CampaignRunner(Duration duration, String frameworkName, Properties frameworkArguments,
                          CampaignConfiguration configuration) {
        if (duration == null || frameworkName == null || frameworkArguments == null || configuration == null) {
            throw new NullPointerException();
        }
        this.configuration = configuration;
        this.duration = duration;
        this.frameworkName = frameworkName;
        this.frameworkArguments = frameworkArguments;
    }

    public void run() throws MojoExecutionException {
        try {
            FuzzFramework framework = createFramework(configuration);
            if (framework.canRestartCampaign()) {
                runWithResets(framework);
            } else {
                if (ProcessUtil.waitFor(framework.startCampaign(), duration.toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new IOException("Campaign process terminated unexpectedly");
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Failed to execute fuzzing campaign", e);
        }
    }

    private FuzzFramework createFramework(CampaignConfiguration config) throws MojoExecutionException {
        try {
            FuzzFramework instance =
                    (FuzzFramework) Class.forName(frameworkName).getDeclaredConstructor().newInstance();
            instance.initialize(config, frameworkArguments);
            return instance;
        } catch (ClassCastException | ReflectiveOperationException | IOException e) {
            throw new MojoExecutionException("Failed to create fuzzing framework instance", e);
        }
    }

    private void runWithResets(FuzzFramework framework) throws IOException, InterruptedException {
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
