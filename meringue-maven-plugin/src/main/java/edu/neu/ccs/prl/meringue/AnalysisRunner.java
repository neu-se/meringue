package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.jacoco.agent.rt.internal_3570298.PreMain;
import org.jacoco.core.analysis.Analyzer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnalysisRunner {
    private final Log log;
    private final boolean debug;
    private final boolean verbose;
    private final int maxTraceSize;
    private final CoverageFilter filter;
    private final File outputDirectory;
    private final File temporaryDirectory;
    private final Duration timeout;

    public AnalysisRunner(Log log, boolean debug, boolean verbose, Duration timeout, int maxTraceSize,
                          CoverageFilter filter, File outputDirectory, File temporaryDirectory) {
        if (log == null || filter == null) {
            throw new NullPointerException();
        }
        if (!outputDirectory.isDirectory() || !temporaryDirectory.isDirectory()) {
            throw new IllegalArgumentException();
        }
        this.log = log;
        this.debug = debug;
        this.verbose = verbose;
        this.timeout = timeout;
        this.maxTraceSize = maxTraceSize;
        this.filter = filter;
        this.outputDirectory = outputDirectory;
        this.temporaryDirectory = temporaryDirectory;
    }

    public void run(CampaignConfiguration configuration, String frameworkName, Properties frameworkArguments,
                    List<JacocoReportFormat> formats) throws MojoExecutionException {
        FuzzFramework framework =
                AbstractMeringueMojo.createFramework(configuration, frameworkName, frameworkArguments);
        log.info("Running analysis for: " + configuration.getTestDescription());
        try {
            File[] inputFiles = collectInputFiles(framework);
            if (inputFiles.length == 0) {
                log.info("No input files were found for analysis");
            }
            framework.startingAnalysis();
            CoverageCalculator calculator = filter.createCoverageCalculator(temporaryDirectory);
            JvmLauncher launcher =
                    createAnalysisLauncher(configuration, framework, filter.getJacocoOption(), debug, verbose,
                                           maxTraceSize, temporaryDirectory);
            CampaignReport report = new CampaignReport(calculator);
            try (CampaignAnalyzer analyzer = new CampaignAnalyzer(launcher, report, timeout.toMillis())) {
                for (int i = 0; i < inputFiles.length; i++) {
                    analyzer.analyze(inputFiles[i]);
                    if ((i + 1) % 100 == 1) {
                        log.info(String.format("Analyzed %d/%d input files", i + 1, inputFiles.length));
                    }
                }
                report = analyzer.getReport();
            }
            report.print(log);
            File configFile = new File(outputDirectory, "config.txt");
            log.info("Writing configuration information to: " + configFile);
            writeConfigurationInfo(configuration, configFile, calculator.getTotalBranches(),
                                   framework.getClass().getName());
            File coverageReportFile = new File(outputDirectory, "coverage.csv");
            File failuresReportFile = new File(outputDirectory, "failures.txt");
            log.info("Writing coverage report to: " + coverageReportFile);
            log.info("Writing failures report to: " + failuresReportFile);
            report.write(coverageReportFile, failuresReportFile);
            File reportDirectory = new File(outputDirectory, "jacoco");
            FileUtil.ensureEmptyDirectory(reportDirectory);
            log.info("Writing JaCoCo reports to: " + reportDirectory);
            for (JacocoReportFormat f : formats) {
                report.writeReport(configuration.getTestDescription(), reportDirectory, f);
            }
        } catch (IOException | ReflectiveOperationException e) {
            throw new MojoExecutionException("Failed to analyze fuzzing campaign", e);
        }
    }

    private void writeConfigurationInfo(CampaignConfiguration config, File file, long totalBranches,
                                        String frameworkName) throws IOException {
        try (PrintStream out = new PrintStream(new BufferedOutputStream(Files.newOutputStream(file.toPath())))) {
            out.printf("test_class_name: %s%n", config.getTestClassName());
            out.printf("test_method_name: %s%n", config.getTestMethodName());
            out.printf("duration_ms: %s%n", config.getDuration().toMillis());
            out.printf("framework: %s%n", frameworkName);
            out.printf("output_directory: %s%n", config.getOutputDir().getAbsolutePath());
            out.printf("java_executable: %s%n", config.getJavaExec().getAbsolutePath());
            out.printf("java_options: %s%n",
                       String.join(" ", config.getJavaOptions())
                             .replaceAll(System.getProperty("line.separator"), " "));
            out.printf("replay_timeout: %d%n", timeout.toMillis());
            out.printf("total_branches: %d%n", totalBranches);
        }
    }

    private static JvmLauncher createAnalysisLauncher(CampaignConfiguration config, FuzzFramework framework,
                                                      String jacocoOption, boolean debug, boolean verbose,
                                                      int maxTraceSize, File temporaryDirectory)
            throws MojoExecutionException, ReflectiveOperationException {
        List<String> options = new LinkedList<>(config.getJavaOptions());
        // Set property to indicate that the analysis phase is running
        options.add("-Dmeringue.analysis=true");
        if (debug) {
            options.add(JvmLauncher.DEBUG_OPT + "5005");
        }
        options.add("-cp");
        options.add(
                AbstractMeringueMojo.buildClassPath(createAnalysisJar(temporaryDirectory), config.getTestClassPathJar(),
                                                    createFrameworkJar(temporaryDirectory, framework)));
        options.add(jacocoOption);
        options.addAll(framework.getAnalysisJavaOptions());
        String[] arguments = new String[]{config.getTestClassName(), config.getTestMethodName(),
                framework.getReplayerClass().getName(), String.valueOf(maxTraceSize)};
        return JvmLauncher.fromMain(config.getJavaExec(), AnalysisForkMain.class.getName(),
                                    options.toArray(new String[0]), debug || verbose, arguments, config.getWorkingDir(),
                                    config.getEnvironment());
    }

    private static File[] collectInputFiles(FuzzFramework framework) throws IOException {
        List<File> files = new LinkedList<>(Arrays.asList(framework.getCorpusFiles()));
        files.addAll(Arrays.asList(framework.getFailureFiles()));
        files.sort(Comparator.comparingLong(File::lastModified));
        return files.toArray(new File[0]);
    }

    private static File createFrameworkJar(File temporaryDirectory, FuzzFramework fuzzFramework)
            throws MojoExecutionException {
        try {
            File jar = new File(temporaryDirectory, "meringue-framework.jar");
            FileUtil.buildManifestJar(fuzzFramework.getRequiredClassPathElements(), jar);
            return jar;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create framework manifest JAR", e);
        }
    }

    private static File createAnalysisJar(File temporaryDirectory) throws MojoExecutionException {
        try {
            File jar = new File(temporaryDirectory, "meringue-analysis.jar");
            FileUtil.buildManifestJar(
                    Stream.of(AnalysisForkMain.class, PreMain.class, Analyzer.class).map(FileUtil::getClassPathElement)
                          .collect(Collectors.toList()), jar);
            return jar;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create analysis manifest JAR", e);
        }
    }
}
