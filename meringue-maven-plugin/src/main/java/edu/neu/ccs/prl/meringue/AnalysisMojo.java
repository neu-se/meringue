package edu.neu.ccs.prl.meringue;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

/**
 * Maven plugin that analyzes the results of a fuzzing campaign.
 */
@Mojo(name = "analyze", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class AnalysisMojo extends AbstractMeringueMojo {
    /**
     * The Maven session.
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;
    /**
     * List of artifacts (in the form groupId:artifactId) to be included in coverage and JaCoCo reports. By default, all
     * test classpath artifacts are included.
     */
    @Parameter(property = "meringue.includedArtifacts")
    List<String> includedArtifacts = new LinkedList<>();
    /**
     * List of class files to include in JaCoCo reports. May use wildcard characters (* and ?). By default, all files
     * are included.
     */
    @Parameter(property = "meringue.inclusions")
    private List<String> inclusions = new LinkedList<>();
    /**
     * List of class files to exclude from JaCoCo reports. May use wildcard characters (* and ?). By default, no files
     * are excluded.
     */
    @Parameter(property = "meringue.exclusions")
    private List<String> exclusions = new LinkedList<>();
    /**
     * Maximum number of frames to include in stack traces taken for failures. By default, a maximum of {@code 5} frames
     * are included.
     */
    @Parameter(property = "meringue.maxTraceSize", defaultValue = "5")
    private int maxTraceSize;
    /**
     * True if the analysis JVM should suspend and wait for a debugger to attach.
     */
    @Parameter(property = "meringue.debug", defaultValue = "false")
    private boolean debug;
    /**
     * Maximum amount of time in seconds to execute a single replayed input or {@code -1} if no timeout should be used.
     * By default, a timeout value of {@code 600} seconds is used.
     */
    @Parameter(property = "meringue.timeout", defaultValue = "600")
    private long timeout;
    /**
     * True if the standard output and error of the forked analysis JVMs should be redirected to the standard out and
     * error of the Maven process. By default, the standard output and error of the forked analysis JVMs is discarded.
     */
    @Parameter(property = "meringue.verbose", defaultValue = "false")
    private boolean verbose;
    /**
     * List of JaCoCo report formats to be generated. Available values are {@code XML}, {@code HTML}, and {@code CSV}.
     * By default, all formats are generated.
     */
    @Parameter(property = "meringue.jacocoFormats", defaultValue = "HTML,CSV,XML")
    private List<JacocoReportFormat> jacocoFormats;
    @Component
    private ArtifactResolver artifactResolver;
    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    @Override
    public void execute() throws MojoExecutionException {
        initialize();
        ArtifactSourceResolver resolver =
                new ArtifactSourceResolver(getLog(), session, artifactResolver, artifactHandlerManager);
        CoverageFilter filter =
                new CoverageFilter(inclusions, exclusions, includedArtifacts, getProject(), resolver);
        AnalysisRunner runner =
                new AnalysisRunner(getLog(), debug, verbose, Duration.ofMillis(timeout), maxTraceSize, filter,
                                   getOutputDirectory(), getTemporaryDirectory());
        runner.run(createConfiguration(), getFramework(), getFrameworkArguments(), jacocoFormats);
    }
}