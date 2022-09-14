package edu.neu.ccs.prl.meringue;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.plugins.dependency.utils.translators.ArtifactTranslator;
import org.apache.maven.plugins.dependency.utils.translators.ClassifierTypeTranslator;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

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
     * List of JARs, directories, and files to be included in the reports.
     */
    @Parameter
    List<File> includedClassPathElements = new LinkedList<>();
    /**
     * List of class files to include in reports. May use wildcard characters (* and ?). By default, all files are
     * included.
     */
    @Parameter
    private List<String> inclusions = new LinkedList<>();
    /**
     * List of class files to exclude from reports. May use wildcard characters (* and ?). By default, no files are
     * excluded.
     */
    @Parameter
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
     * The output formats of JaCoCo report. Available values are {@code XML}, {@code HTML}, and {@code CSV}.
     * By default, the format is set to {@code HTML,CSV,XML}.
     */
    @Parameter(property = "meringue.outputJaCoCoFormat", defaultValue = "HTML,CSV,XML")
    private List<ReportFormat> formats;

    @Component
    private ArtifactResolver artifactResolver;
    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Running analysis for: " + getTestDescription());
        try {
            CampaignConfiguration config = createConfiguration();
            FuzzFramework framework = createFramework(config);
            File[] inputFiles = collectInputFiles(framework);
            if (inputFiles.length == 0) {
                getLog().info("No input files were found for analysis");
            }
            CoverageFilter filter = new CoverageFilter(inclusions, exclusions, includedClassPathElements);
            JvmLauncher launcher = createLauncher(config, framework, filter);
            CoverageCalculator calculator = filter.createCoverageCalculator(getTestClassPathElements());
            CampaignReport report = new CampaignReport(calculator, getSources());
            try (CampaignAnalyzer analyzer = new CampaignAnalyzer(launcher, report, timeout)) {
                for (int i = 0; i < inputFiles.length; i++) {
                    analyzer.analyze(inputFiles[i]);
                    if ((i + 1) % 100 == 1) {
                        System.out.printf("Analyzed %d/%d input files%n", i + 1, inputFiles.length);
                    }
                }
                report = analyzer.getReport();
            }
            report.print(getLog());
            File configFile = new File(getOutputDir(), "config.txt");
            getLog().info("Writing configuration information to: " + configFile);
            writeConfigurationInfo(config, configFile, calculator.getTotalBranches());
            File coverageReportFile = new File(getOutputDir(), "coverage.csv");
            File failuresReportFile = new File(getOutputDir(), "failures.txt");
            getLog().info("Writing coverage report to: " + coverageReportFile);
            getLog().info("Writing failures report to: " + failuresReportFile);
            report.write(coverageReportFile, failuresReportFile);
            File reportDir = new File(getOutputDir(), "jacoco-report");
            getLog().info("Writing JaCoCo report to: " + reportDir);
            reportDir.mkdirs();
            for (ReportFormat f: formats) {
                report.writeReport(getTestDescription(), reportDir, f);
            }
        } catch (IOException | ReflectiveOperationException e) {
            throw new MojoExecutionException("Failed to analyze fuzzing campaign", e);
        }
    }

    private void writeConfigurationInfo(CampaignConfiguration config, File file, long totalBranches)
            throws IOException {
        try (PrintStream out = new PrintStream(new BufferedOutputStream(Files.newOutputStream(file.toPath())))) {
            out.printf("test_class_name: %s%n", config.getTestClassName());
            out.printf("test_method_name: %s%n", config.getTestMethodName());
            out.printf("duration_ms: %s%n", config.getDuration().toMillis());
            out.printf("framework: %s%n", getFramework());
            out.printf("output_directory: %s%n", config.getOutputDir().getAbsolutePath());
            out.printf("java_executable: %s%n", config.getJavaExec().getAbsolutePath());
            out.printf("java_options: %s%n",
                       String.join(" ", config.getJavaOptions()).replaceAll(System.getProperty("line.separator"), " "));
            out.printf("replay_timeout: %d%n", timeout);
            out.printf("total_branches: %d%n", totalBranches);
        }
    }

    private JvmLauncher createLauncher(CampaignConfiguration config, FuzzFramework framework, CoverageFilter filter)
            throws MojoExecutionException, ReflectiveOperationException, IOException {
        List<String> options = new LinkedList<>(config.getJavaOptions());
        options.addAll(framework.prepareForAnalysisPhase());
        // Set property to indicate that the analysis phase is running
        options.add("-Dmeringue.analysis=true");
        if (debug) {
            options.add(JvmLauncher.DEBUG_OPT + "5005");
        }
        options.add("-cp");
        options.add(buildClassPath(createAnalysisJar(), config.getTestClassPathJar(), createFrameworkJar(framework)));
        options.add(filter.getJacocoOption());
        String[] arguments = new String[]{config.getTestClassName(), config.getTestMethodName(),
                framework.getReplayerClass().getName(), String.valueOf(maxTraceSize)};
        return new JvmLauncher.JavaMainLauncher(config.getJavaExec(), AnalysisForkMain.class.getName(),
                                                options.toArray(new String[0]),
                                                debug | Boolean.getBoolean("meringue.verbose"), arguments);
    }

    private File[] getSources() {
        Set<Artifact> artifacts =
                getProject().getArtifacts().stream().filter(a -> a.getArtifactHandler().isAddedToClasspath())
                            .collect(Collectors.toSet());
        ArtifactTranslator translator = new ClassifierTypeTranslator(artifactHandlerManager, "sources", "");
        Collection<ArtifactCoordinate> coordinates = translator.translate(artifacts, getLog());
        Set<Artifact> testSources = resolve(new LinkedHashSet<>(coordinates));
        Set<File> sourceDirs = testSources.stream().map(Artifact::getFile).collect(Collectors.toSet());
        sourceDirs.add(new File(getProject().getBuild().getTestSourceDirectory()));
        sourceDirs.add(new File(getProject().getBuild().getSourceDirectory()));
        return sourceDirs.stream().filter(f -> f != null && f.exists()).toArray(File[]::new);
    }

    private Set<Artifact> resolve(Set<ArtifactCoordinate> coordinates) {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        buildingRequest.setRemoteRepositories(getProject().getRemoteArtifactRepositories());
        Set<Artifact> result = new LinkedHashSet<>();
        for (ArtifactCoordinate coordinate : coordinates) {
            try {
                result.add(artifactResolver.resolveArtifact(buildingRequest, coordinate).getArtifact());
            } catch (ArtifactResolverException ex) {
                getLog().debug("error resolving: " + coordinate);
            }
        }
        return result;
    }

    private static File[] collectInputFiles(FuzzFramework framework) throws IOException {
        List<File> files = new LinkedList<>(Arrays.asList(framework.getCorpusFiles()));
        files.addAll(Arrays.asList(framework.getFailureFiles()));
        files.sort(Comparator.comparingLong(File::lastModified));
        return files.toArray(new File[0]);
    }
}