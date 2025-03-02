package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.jacoco.agent.rt.internal_aeaf9ab.PreMain;
import org.jacoco.core.analysis.Analyzer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface AnalysisValues extends CampaignValues {
    /**
     * List of artifacts (in the form groupId:artifactId) to be included in coverage and JaCoCo reports. If empty, all
     * test classpath artifacts are included.
     */
    List<String> getIncludedArtifacts() throws MojoExecutionException;

    /**
     * List of class files to include in coverage and JaCoCo reports. May use wildcard characters (* and ?). If empty,
     * all files are included.
     */
    List<String> getInclusions() throws MojoExecutionException;

    /**
     * List of class files to exclude from coverage and JaCoCo reports. May use wildcard characters (* and ?). If empty,
     * no files are excluded.
     */
    List<String> getExclusions() throws MojoExecutionException;

    /**
     * Maximum number of frames to include in stack traces taken for failures.
     */
    int getMaxTraceSize() throws MojoExecutionException;

    /**
     * True if analysis JVMs should suspend and wait for a debugger to attach.
     */
    boolean isDebug() throws MojoExecutionException;

    /**
     * True if the standard output and error of analysis JVMs should be redirected to the standard out and error of the
     * Maven process. Otherwise, the standard output and error of analysis JVMs is discarded.
     */
    boolean isVerbose() throws MojoExecutionException;

    /**
     * List of JaCoCo report formats to be generated.
     */
    List<JacocoReportFormat> getJacocoFormats() throws MojoExecutionException;

    /**
     * Returns the maximum amount of time in seconds to execute a single replayed input or {@code -1} if no timeout
     * should be used.
     */
    long getTimeout() throws MojoExecutionException;

    /**
     * Artifact resolver for this Maven session.
     */
    ArtifactResolver getArtifactResolver() throws MojoExecutionException;

    default ArtifactSourceResolver createArtifactSourceResolver() throws MojoExecutionException {
        return new ArtifactSourceResolver(getLog(), getSession(), getArtifactResolver(), getArtifactHandlerManager());
    }

    default CoverageCalculator createCoverageCalculator() throws MojoExecutionException {
        try {
            return new CoverageFilter(this)
                    .createCoverageCalculator(getTemporaryDirectory());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create coverage calculator", e);
        }
    }

    /**
     * True if classes from the Java Class Library should be included in coverage and JaCoCo reports.
     *
     * @return true if classes from the Java Class Library should be included in coverage and JaCoCo reports.
     */
    default boolean includeJavaClassLibrary() {
        return false;
    }

    default void analyze() throws MojoExecutionException {
        new AnalysisRunner(this).run();
    }

    default JvmLauncher createAnalysisLauncher(String jacocoOption, CampaignConfiguration configuration,
                                               FuzzFramework framework)
            throws MojoExecutionException, ReflectiveOperationException {
        List<String> options = new LinkedList<>(configuration.getJavaOptions());
        // Set property to indicate that the analysis phase is running
        options.add("-Dmeringue.analysis=true");
        // Prevent stack traces from being omitted
        options.add("-XX:-OmitStackTraceInFastThrow");
        if (isDebug()) {
            options.add(JvmLauncher.DEBUG_OPT + "5005");
        }
        options.add("-cp");
        options.add(CampaignUtil.buildClassPath(
                createAnalysisJar(),
                configuration.getTestClasspathJar(),
                createAnalysisFrameworkJar(framework)
        ));
        options.add(jacocoOption);
        options.addAll(framework.getAnalysisJavaOptions());
        String[] arguments = new String[]{
                configuration.getTestClassName(),
                configuration.getTestMethodName(),
                framework.getReplayerClass().getName(),
                String.valueOf(getMaxTraceSize())
        };
        return JvmLauncher.fromMain(
                configuration.getJavaExecutable(),
                AnalysisForkMain.class.getName(),
                options.toArray(new String[0]),
                isDebug() || isVerbose(),
                arguments,
                configuration.getWorkingDirectory(),
                configuration.getEnvironment()
        );
    }

    default File createAnalysisJar() throws MojoExecutionException {
        try {
            File jar = new File(getTemporaryDirectory(), "meringue-analysis.jar");
            Collection<File> elements = Stream.of(AnalysisForkMain.class, PreMain.class, Analyzer.class)
                                              .map(FileUtil::getClassPathElement)
                                              .collect(Collectors.toCollection(HashSet::new));
            FileUtil.buildManifestJar(elements, jar);
            return jar;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create analysis manifest JAR", e);
        }
    }

    default File createAnalysisFrameworkJar(FuzzFramework framework) throws MojoExecutionException {
        try {
            File jar = new File(getTemporaryDirectory(), "meringue-analysis-framework.jar");
            FileUtil.buildManifestJar(framework.getRequiredClassPathElements(), jar);
            return jar;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create framework manifest JAR", e);
        }
    }
}
