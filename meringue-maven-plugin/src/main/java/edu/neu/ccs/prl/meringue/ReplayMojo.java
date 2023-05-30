package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

/**
 * Maven plugin that replays a single input from a fuzzing campaign.
 */
@Mojo(name = "replay", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class ReplayMojo extends AbstractMeringueMojo implements CampaignValues {
    /**
     * Input to be replayed.
     */
    @Parameter(property = "meringue.input", required = true)
    private File input;

    @Override
    public void execute() throws MojoExecutionException {
        new ReplayRunner(this, input).run();
    }
}