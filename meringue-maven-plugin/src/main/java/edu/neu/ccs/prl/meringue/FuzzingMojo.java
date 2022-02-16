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
            FileUtil.createOrCleanDirectory(getOutputDir());
            CampaignConfiguration config = createConfiguration();
            Process process = createFramework(config).startCampaign();
            if (ProcessUtil.waitFor(process, getDuration().toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IOException("Campaign process terminated unexpectedly");
            }
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Failed to execute fuzzing campaign", e);
        }
    }
}