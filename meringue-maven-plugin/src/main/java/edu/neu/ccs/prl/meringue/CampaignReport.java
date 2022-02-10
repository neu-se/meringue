package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.logging.Log;

import java.io.*;
import java.util.List;
import java.util.Set;

public final class CampaignReport {
    private final long totalBranches;
    private final long[] times;
    private final long[] branchCoverageValues;
    private final Set<List<StackTraceElement>> uniqueTraces;

    CampaignReport(long totalBranches, long[] times, long[] branchCoverageValues,
                   Set<List<StackTraceElement>> uniqueTraces) {
        this.totalBranches = totalBranches;
        this.times = times;
        this.branchCoverageValues = branchCoverageValues;
        this.uniqueTraces = uniqueTraces;
    }

    public void print(Log log) {
        long last = branchCoverageValues.length > 0 ? branchCoverageValues[branchCoverageValues.length - 1] : 0;
        long total = totalBranches;
        log.info(String.format("Hit branches: %d/%d = %.7f", last, total, (1.0 * last) / total));
        for (List<StackTraceElement> uniqueTrace : uniqueTraces) {
            Throwable t = new Throwable();
            t.setStackTrace(uniqueTrace.toArray(new StackTraceElement[0]));
            t.printStackTrace();
        }
    }

    public void write(File coverageReportFile, File failuresReportFile) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(coverageReportFile)))) {
            out.printf("time, branches (out of %d)%n", totalBranches);
            for (int i = 0; i < times.length; i++) {
                out.printf("%d, %d%n", times[i], branchCoverageValues[i]);
            }
        }
        try (PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(failuresReportFile)))) {
            int i = 1;
            for (List<StackTraceElement> uniqueTrace : uniqueTraces) {
                out.printf("%d. %s%n", i++, uniqueTrace);
            }
        }
    }
}
