package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
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
        File[] inputFiles = collectInputFiles(framework);
        if (inputFiles.length == 0) {
            values.getLog().info("No input files were found for analysis");
        }
        framework.startingAnalysis();
        CoverageCalculator calculator = values.createCoverageCalculator();
        JvmLauncher launcher = values.createAnalysisLauncher(calculator.getFilter().getJacocoOption(),
                                                             configuration, framework);
        CampaignReport report = new CampaignReport(calculator);
        try (CampaignAnalyzer analyzer = new CampaignAnalyzer(launcher, report, values.getTimeout())) {
            for (int i = 0; i < inputFiles.length; i++) {
                analyzer.analyze(inputFiles[i]);
                if ((i + 1) % 100 == 1) {
                    values.getLog().info(String.format("Analyzed %d/%d input files", i + 1, inputFiles.length));
                }
            }
            report = analyzer.getReport();
        }
        writeReports(configuration, calculator, report);
    }

    private void writeReports(CampaignConfiguration configuration, CoverageCalculator calculator, CampaignReport report)
            throws IOException, MojoExecutionException {
        report.print(values.getLog());
        File summaryFile = new File(values.getOutputDirectory(), "config.txt");
        values.getLog().info("Writing configuration information to: " + summaryFile);
        writeConfigurationInfo(configuration, summaryFile, calculator.getTotalBranches());
        File coverageReportFile = new File(values.getOutputDirectory(), "coverage.csv");
        File failuresReportFile = new File(values.getOutputDirectory(), "failures.txt");
        values.getLog().info("Writing coverage report to: " + coverageReportFile);
        values.getLog().info("Writing failures report to: " + failuresReportFile);
        report.write(coverageReportFile, failuresReportFile);
        File reportDirectory = new File(values.getOutputDirectory(), "jacoco");
        FileUtil.ensureEmptyDirectory(reportDirectory);
        values.getLog().info("Writing JaCoCo reports to: " + reportDirectory);
        for (JacocoReportFormat f : values.getJacocoFormats()) {
            report.writeReport(values.getTestDescription(), reportDirectory, f);
        }
    }

    private void writeConfigurationInfo(CampaignConfiguration configuration, File file, long totalBranches)
            throws IOException, MojoExecutionException {
        try (PrintStream out = new PrintStream(new BufferedOutputStream(Files.newOutputStream(file.toPath())))) {
            out.printf("test_class_name: %s%n", configuration.getTestClassName());
            out.printf("test_method_name: %s%n", configuration.getTestMethodName());
            out.printf("duration_ms: %s%n", configuration.getDuration().toMillis());
            out.printf("framework: %s%n", values.getFrameworkClassName());
            out.printf("java_executable: %s%n", configuration.getJavaExecutable().getAbsolutePath());
            String javaOptionsString =
                    String.join(" ", configuration.getJavaOptions()).replaceAll(System.getProperty("line.separator"),
                                                                                " ");
            out.printf("java_options: %s%n", javaOptionsString);
            out.printf("replay_timeout: %d%n", values.getTimeout());
            out.printf("total_branches: %d%n", totalBranches);
        }
    }

    private static File[] collectInputFiles(FuzzFramework framework) throws IOException {
        List<File> files = new LinkedList<>(Arrays.asList(framework.getCorpusFiles()));
        files.addAll(Arrays.asList(framework.getFailureFiles()));
        files.sort(Comparator.comparingLong(File::lastModified));
        return files.toArray(new File[0]);
    }
}
