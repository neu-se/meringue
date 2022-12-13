package edu.neu.ccs.prl.meringue.report;

import edu.neu.ccs.prl.meringue.CampaignConfiguration;
import edu.neu.ccs.prl.meringue.ClassFilter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.time.Duration;

/**
 * Record type used for JSON reports.
 * <p>
 * Immutable.
 */
@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class SummaryReport {
    private final CampaignConfiguration configuration;
    private final String frameworkClassName;
    private final ClassFilter coverageFilter;
    private final int maxTraceSize;
    private final Duration replayTimeout;
    private final long totalBranches;
    private final long numberOfCoveredBranches;
    private final int numberOfUniqueFailures;
    private final int totalSavedInputs;

    public SummaryReport(CampaignConfiguration configuration, String frameworkClassName, ClassFilter coverageFilter,
                         int maxTraceSize, Duration replayTimeout, long totalBranches, long numberOfCoveredBranches,
                         int numberOfUniqueFailures, int totalSavedInputs) {
        this.configuration = configuration;
        this.frameworkClassName = frameworkClassName;
        this.coverageFilter = coverageFilter;
        this.maxTraceSize = maxTraceSize;
        this.replayTimeout = replayTimeout;
        this.totalBranches = totalBranches;
        this.numberOfCoveredBranches = numberOfCoveredBranches;
        this.numberOfUniqueFailures = numberOfUniqueFailures;
        this.totalSavedInputs = totalSavedInputs;
    }

    public CampaignConfiguration getConfiguration() {
        return configuration;
    }

    public void writeLegacyReport(File file) throws IOException {
        try (PrintStream out = new PrintStream(new BufferedOutputStream(Files.newOutputStream(file.toPath())))) {
            out.printf("test_class_name: %s%n", configuration.getTestClassName());
            out.printf("test_method_name: %s%n", configuration.getTestMethodName());
            out.printf("duration_ms: %s%n", configuration.getDuration().toMillis());
            out.printf("framework: %s%n", frameworkClassName);
            out.printf("java_executable: %s%n", configuration.getJavaExecutable().getAbsolutePath());
            out.printf("total_branches: %d%n", totalBranches);
        }
    }

    public void write(File file) throws IOException {
        ReportUtil.writeJson(file, SummaryReport.class, this);
    }

    public long getTotalBranches() {
        return totalBranches;
    }

    public long getNumberOfCoveredBranches() {
        return numberOfCoveredBranches;
    }

    public int getNumberOfUniqueFailures() {
        return numberOfUniqueFailures;
    }
}
