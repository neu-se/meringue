package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Maven plugin that analyzes the results of a fuzzing campaign.
 */
@Mojo(name = "analyze", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class AnalysisMojo extends AbstractMeringueMojo {
    /**
     * List of JARs, directories, and files to be included in the reports.
     */
    @Parameter
    List<File> includedClassPathElements = new LinkedList<>();
    @Parameter(defaultValue = "${project.build.sourceDirectory}", readonly = true, required = true)
    private File appSourcesDir;
    @Parameter(defaultValue = "${project.build.testSourceDirectory}", readonly = true, required = true)
    private File testSourcesDir;
    /**
     * List of class files to include in reports. May use wildcard
     * characters (* and ?). By default, all files are included.
     */
    @Parameter
    private List<String> inclusions = new LinkedList<>();
    /**
     * List of class files to exclude from reports. May use wildcard
     * characters (* and ?). By default, no files are excluded.
     */
    @Parameter
    private List<String> exclusions = new LinkedList<>();
    /**
     * Maximum number of frames to include in stack traces taken for failures.
     * By default, a maximum of {@code 5} frames are included.
     */
    @Parameter(property = "meringue.maxTraceSize", defaultValue = "5")
    private int maxTraceSize;
    /**
     * True if the analysis JVM should suspend and wait for a debugger to attach.
     */
    @Parameter(property = "meringue.debug", defaultValue = "false")
    private boolean debug;
    /**
     * Directory containing source code archives and directories.
     */
    @Parameter(property = "meringue.sources")
    private File sources;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Running analysis for: " + getTestDescription());
        try {
            CampaignConfiguration config = createConfiguration();
            FuzzFramework framework = createFramework(config);
            File[] inputFiles = collectInputFiles(framework);
            if (inputFiles.length == 0) {
                getLog().info("No input files were found for analysis");
                return;
            }
            if (sources != null && !sources.isDirectory()) {
                getLog().warn("Invalid sources directory: " + sources);
                sources = null;
            }
            CoverageFilter filter = new CoverageFilter(inclusions, exclusions, includedClassPathElements);
            JvmLauncher launcher = createLauncher(config, framework, filter);
            CoverageCalculator calculator = filter.createCoverageCalculator(getTestClassPathElements());
            List<File> sourceDirs = new LinkedList<>(Arrays.asList(testSourcesDir, appSourcesDir));
            if (sources != null) {
                sourceDirs.addAll(Arrays.asList(Objects.requireNonNull(sources.listFiles())));
            }
            CampaignReport report = new CampaignReport(calculator,
                    sourceDirs.stream().filter(f -> f != null && f.exists()).toArray(File[]::new));
            try (CampaignAnalyzer analyzer = new CampaignAnalyzer(launcher, report)) {
                for (int i = 0; i < inputFiles.length; i++) {
                    analyzer.analyze(inputFiles[i]);
                    if ((i + 1) % 100 == 0) {
                        System.out.printf("Analyzed %d/%d input files%n", i + 1, inputFiles.length);
                    }
                }
                report = analyzer.getReport();
            }
            report.print(getLog());
            File coverageReportFile = new File(getOutputDir(), "coverage.csv");
            File failuresReportFile = new File(getOutputDir(), "failures.txt");
            getLog().info("Writing coverage report to: " + coverageReportFile);
            getLog().info("Writing failures report to: " + failuresReportFile);
            report.write(coverageReportFile, failuresReportFile);
            File htmlReportDir = new File(getOutputDir(), "jacoco-report");
            getLog().info("Writing JaCoCo report to: " + htmlReportDir);
            report.writeHtmlReport(getTestDescription(), htmlReportDir);
        } catch (IOException | ReflectiveOperationException e) {
            throw new MojoExecutionException("Failed to analyze fuzzing campaign", e);
        }
    }

    private JvmLauncher createLauncher(CampaignConfiguration config, FuzzFramework framework, CoverageFilter filter)
            throws MojoExecutionException, ReflectiveOperationException {
        List<String> options = new LinkedList<>(config.getJavaOptions());
        if (debug) {
            options.add(JvmLauncher.DEBUG_OPT + "5005");
        }
        options.add("-cp");
        options.add(buildClassPath(createAnalysisJar(), config.getTestClassPathJar(), createFrameworkJar(framework)));
        options.add(filter.getJacocoOption());
        String[] arguments = new String[]{config.getTestClassName(), config.getTestMethodName(),
                framework.getReplayerClass().getName(), String.valueOf(maxTraceSize)};
        return new JvmLauncher.JavaMainLauncher(
                config.getJavaExec(),
                AnalysisForkMain.class.getName(),
                options.toArray(new String[0]),
                debug | Boolean.getBoolean("meringue.verbose"),
                arguments
        );
    }

    private static File[] collectInputFiles(FuzzFramework framework) throws IOException {
        List<File> files = new LinkedList<>(Arrays.asList(framework.getCorpusFiles()));
        files.addAll(Arrays.asList(framework.getFailureFiles()));
        files.sort(Comparator.comparingLong(File::lastModified));
        return files.toArray(new File[0]);
    }
}