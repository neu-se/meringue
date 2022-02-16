package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

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

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Running analysis for: " + getTestDescription());
        try {
            CampaignConfiguration config = createConfiguration();
            FuzzFramework framework = createFramework(config);
            List<File> inputFiles = collectInputFiles(framework);
            if (inputFiles.isEmpty()) {
                getLog().info("No input files were found for analysis");
                return;
            }
            CoverageFilter filter = new CoverageFilter(inclusions, exclusions, includedClassPathElements);
            JvmLauncher launcher = createLauncher(config, framework, filter);
            // Create a server socket bound to an automatically allocated port
            try (ServerSocket server = new ServerSocket(0)) {
                // Launch the analysis JVM
                Process process = launcher.launch(new String[]{String.valueOf(server.getLocalPort())});
                CoverageCalculator calculator = filter.createCoverageCalculator(getTestClassPathElements());
                CampaignReport report = new CampaignReport(calculator.getTotalBranches());
                try (ForkConnection connection = new ForkConnection(server.accept())) {
                    configureFork(config, framework, inputFiles, connection);
                    analyze(inputFiles, connection, report, calculator);
                }
                if (ProcessUtil.waitFor(process) != 0) {
                    throw new IOException("Error occurred in forked analysis process");
                }
                report.print(getLog());
                File coverageReportFile = new File(getOutputDir(), "coverage.csv");
                File failuresReportFile = new File(getOutputDir(), "failures.txt");
                getLog().info("Writing coverage report to: " + coverageReportFile);
                getLog().info("Writing failures report to: " + failuresReportFile);
                report.write(coverageReportFile, failuresReportFile);
            }
        } catch (IOException | InterruptedException | ReflectiveOperationException e) {
            throw new MojoExecutionException("Failed to analyze fuzzing campaign", e);
        }
    }

    private JvmLauncher createLauncher(CampaignConfiguration config, FuzzFramework framework, CoverageFilter filter)
            throws MojoExecutionException {
        List<String> options = new LinkedList<>(config.getJavaOptions());
        if (debug) {
            options.add(JvmLauncher.DEBUG_OPT + "5005");
        }
        options.add("-cp");
        options.add(buildClassPath(createAnalysisJar(), config.getTestClassPathJar(), createFrameworkJar(framework)));
        options.add(filter.getJacocoOption());
        return new JvmLauncher.JavaMainLauncher(
                config.getJavaExec(),
                AnalysisForkMain.class.getName(),
                options.toArray(new String[0]),
                debug,
                new String[0]
        );
    }

    private void configureFork(CampaignConfiguration config, FuzzFramework framework, List<File> inputFiles,
                               ForkConnection connection) throws IOException, ReflectiveOperationException {
        connection.send(config.getTestClassName());
        connection.send(config.getTestMethodName());
        connection.send(framework.getReplayerClass().getName());
        connection.send(maxTraceSize);
        connection.send(inputFiles.toArray(new File[0]));
    }

    private void analyze(List<File> inputFiles, ForkConnection connection, CampaignReport report,
                         CoverageCalculator calculator) throws IOException, InterruptedException,
            ReflectiveOperationException {
        long firstTimestamp = inputFiles.isEmpty() ? 0 : inputFiles.get(0).lastModified();
        int i = 0;
        for (File inputFile : inputFiles) {
            long time = inputFile.lastModified() - firstTimestamp;
            byte[] execData = connection.receive(byte[].class);
            report.recordCoverage(time, calculator.calculate(execData));
            if (connection.receive(Boolean.class)) {
                StackTraceElement[] trace = connection.receive(StackTraceElement[].class);
                report.recordFailure(inputFile, trace);
            }
            if ((i + 1) % 100 == 0) {
                System.out.printf("Analyzed %d/%d input files%n", i + 1, inputFiles.size());
            }
            i++;
        }
        // Send the shutdown signal
        connection.send(null);
    }


    private static List<File> collectInputFiles(FuzzFramework framework) throws IOException {
        List<File> files = new LinkedList<>(Arrays.asList(framework.getCorpusFiles()));
        files.addAll(Arrays.asList(framework.getFailureFiles()));
        files.sort(Comparator.comparingLong(File::lastModified));
        return files;
    }
}