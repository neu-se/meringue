package edu.neu.ccs.prl.meringue;

import edu.neu.ccs.prl.meringue.internal.CampaignForkMain;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Maven plugin that runs a fuzzing campaign.
 */
@Mojo(name = "fuzz", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class FuzzingMojo extends AbstractMeringueMojo {
    /**
     * Textual representation of the maximum amount of time to execute the fuzzing campaign in the ISO-8601 duration
     * format. The default value is one day.
     * <p>
     * {@link java.time.Duration#parse(CharSequence)}
     */
    @Parameter(property = "meringue.duration", defaultValue = "P1D")
    private String duration;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            getLog().info("Running fuzzing campaign: " + getTestDescription());
            PluginUtil.createOrCleanDirectory(getOutputDir());
            CampaignConfiguration config = createConfiguration(CampaignForkMain.class);
            execute(config, createFramework(), Duration.parse(duration), getOptions());
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Failed to execute fuzzing campaign", e);
        }
    }

    private static void execute(CampaignConfiguration config, FuzzFramework framework, Duration duration,
                                Properties options)
            throws IOException, InterruptedException {
        Process process = framework.createCampaignProcess(config, duration, options);
        if (ProcessUtil.waitFor(process, duration.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new IOException("Campaign process terminated unexpectedly");
        }
    }
}