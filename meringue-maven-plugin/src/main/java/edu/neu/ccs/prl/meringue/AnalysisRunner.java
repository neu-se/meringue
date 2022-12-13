package edu.neu.ccs.prl.meringue;

import edu.neu.ccs.prl.meringue.report.CoverageReport;
import edu.neu.ccs.prl.meringue.report.FailureReport;
import edu.neu.ccs.prl.meringue.report.SummaryReport;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class AnalysisRunner {
    private final AnalysisValues values;

    public AnalysisRunner(AnalysisValues values) {
        if (values == null) {
            throw new NullPointerException();
        }
        this.values = values;
    }

    public void run() throws MojoExecutionException {
        try {
            values.initialize();
            CampaignConfiguration configuration = values.createCampaignConfiguration();
            FuzzFramework framework = values.createFrameworkBuilder().build(configuration);
            values.getLog().info("Running analysis for: " + values.getTestDescription());
            run(framework, configuration);
        } catch (IOException | ReflectiveOperationException e) {
            throw new MojoExecutionException("Failed to analyze fuzzing campaign", e);
        }
    }

    private void run(FuzzFramework framework, CampaignConfiguration configuration)
            throws MojoExecutionException, IOException, ReflectiveOperationException {
        framework.startingAnalysis();
        CoverageCalculator calculator = values.createCoverageCalculator();
        JvmLauncher launcher =
                values.createAnalysisLauncher(calculator.getFilter().getJacocoOption(), configuration, framework);
        File[] inputFiles = collectInputFiles(framework);
        long firstTimestamp = inputFiles.length == 0 ? 0 : inputFiles[0].lastModified();
        CoverageReport coverageReport = new CoverageReport(calculator, firstTimestamp);
        FailureReport failureReport = new FailureReport(firstTimestamp);
        analyze(inputFiles, launcher, coverageReport, failureReport);
        SummaryReport summaryReport = new SummaryReport(
                configuration,
                framework.getClass().getName(),
                calculator.getFilter().getClassFilter(),
                values.getMaxTraceSize(),
                Duration.ofSeconds(values.getTimeout()),
                coverageReport.getTotalBranches(),
                coverageReport.getCoveredBranches(),
                failureReport.getNumberOfUniqueFailures(),
                inputFiles.length
        );
        logResults(summaryReport);
        writeSummaryReport(summaryReport);
        writeCoverageReport(coverageReport);
        writeFailureReport(failureReport);
        writeJacocoReports(configuration, coverageReport);
    }

    private void analyze(File[] inputFiles, JvmLauncher launcher, CoverageReport coverageReport,
                         FailureReport failureReport) throws IOException, MojoExecutionException {
        if (inputFiles.length == 0) {
            values.getLog().info("No input files were found for analysis");
            return;
        }
        try (CampaignAnalyzer analyzer = new CampaignAnalyzer(launcher, coverageReport, failureReport,
                                                              values.getTimeout())) {
            for (int i = 0; i < inputFiles.length; i++) {
                analyzer.analyze(inputFiles[i]);
                if ((i + 1) % 100 == 1) {
                    values.getLog().info(String.format("Analyzed %d/%d input files", i + 1, inputFiles.length));
                }
            }
        }
    }

    private void logResults(SummaryReport report) throws MojoExecutionException {
        long covered = report.getNumberOfCoveredBranches();
        long total = report.getTotalBranches();
        values.getLog().info(String.format("Hit branches: %d/%d = %.7f", covered, total, (1.0 * covered) / total));
        values.getLog().info("Unique failures observed: " + report.getNumberOfUniqueFailures());
    }

    private void writeJacocoReports(CampaignConfiguration configuration, CoverageReport report)
            throws MojoExecutionException, IOException {
        File directory = new File(values.getOutputDirectory(), "jacoco");
        FileUtil.ensureEmptyDirectory(directory);
        values.getLog().info("Writing JaCoCo reports to: " + directory);
        report.writeJacocoReports(configuration.getTestDescription(), directory, values.getJacocoFormats());
    }

    private void writeCoverageReport(CoverageReport report) throws MojoExecutionException, IOException {
        File file = new File(values.getOutputDirectory(), "coverage.csv");
        values.getLog().info("Writing coverage report to: " + file);
        report.write(file);
    }

    private void writeFailureReport(FailureReport report) throws MojoExecutionException, IOException {
        File file = new File(values.getOutputDirectory(), "failures.json");
        values.getLog().info("Writing failure report to: " + file);
        report.write(file);
    }

    private void writeSummaryReport(SummaryReport report) throws IOException, MojoExecutionException {
        File file = new File(values.getOutputDirectory(), "summary.json");
        values.getLog().info("Writing summary report to: " + file);
        report.write(file);
    }

    private static File[] collectInputFiles(FuzzFramework framework) throws IOException {
        List<File> files = new LinkedList<>(Arrays.asList(framework.getCorpusFiles()));
        files.addAll(Arrays.asList(framework.getFailureFiles()));
        files.sort(Comparator.comparingLong(File::lastModified));
        return files.toArray(new File[0]);
    }
}
