package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Maven plugin that runs a fuzzing campaign.
 */
@Mojo(name = "fuzz", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class FuzzingMojo extends AbstractMeringueMojo {
    @Override
    public void execute() throws MojoExecutionException {
        try {
            getLog().info("Running fuzzing campaign: " + getTestDescription());
            FileUtil.ensureEmptyDirectory(getOutputDir());
            CampaignConfiguration config = createConfiguration();
            FuzzFramework framework = createFramework(config);
            if (framework.canRestartCampaign()) {
                runWithResets(framework);
            } else {
                if (ProcessUtil.waitFor(framework.startCampaign(), getDuration().toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new IOException("Campaign process terminated unexpectedly");
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Failed to execute fuzzing campaign", e);
        }
    }

    private void runWithResets(FuzzFramework framework) throws IOException, InterruptedException {
        long endTime = System.currentTimeMillis() + getDuration().toMillis();
        if (!ProcessUtil.waitFor(framework.startCampaign(), getDuration().toMillis(), TimeUnit.MILLISECONDS)) {
            return;
        } else {
            getLog().info("Restarting campaign");
        }
        while (endTime > System.currentTimeMillis()) {
            long remaining = endTime - System.currentTimeMillis();
            if (!ProcessUtil.waitFor(framework.restartCampaign(), remaining, TimeUnit.MILLISECONDS)) {
                return;
            }
        }
    }
}