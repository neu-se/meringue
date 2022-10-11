package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.IOException;

/**
 * Maven plugin that runs a fuzzing campaign.
 */
@Mojo(name = "fuzz", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class FuzzingMojo extends AbstractMeringueMojo {
    @Override
    public void execute() throws MojoExecutionException {
        try {
            FileUtil.ensureEmptyDirectory(getOutputDir());
            CampaignConfiguration config = createConfiguration();
            CampaignRunner runner = new CampaignRunner(getDuration(), getFramework(), getFrameworkArguments(), config);
            getLog().info("Running fuzzing campaign: " + getTestDescription());
            runner.run();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to execute fuzzing campaign", e);
        }
    }
}