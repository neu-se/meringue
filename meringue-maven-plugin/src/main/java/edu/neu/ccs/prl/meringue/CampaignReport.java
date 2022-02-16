package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.logging.Log;

import java.io.*;
import java.util.*;

final class CampaignReport {
    private final Map<List<StackTraceElement>, List<File>> failureMap = new HashMap<>();
    private final long totalBranches;
    private final List<long[]> rows = new ArrayList<>();

    public CampaignReport(long totalBranches) {
        this.totalBranches = totalBranches;
    }

    public void recordCoverage(long time, long coveredBranches) {
        rows.add(new long[]{time, coveredBranches});
    }

    public void recordFailure(File inputFile, StackTraceElement[] trace) {
        List<StackTraceElement> elements = Arrays.asList(trace);
        if (!failureMap.containsKey(elements)) {
            failureMap.put(elements, new ArrayList<>());
        }
        failureMap.get(elements).add(inputFile);
    }

    public void print(Log log) {
        if (!rows.isEmpty()) {
            long finalCoverage = rows.get(rows.size() - 1)[1];
            long total = totalBranches;
            log.info(String.format("Hit branches: %d/%d = %.7f", finalCoverage, total, (1.0 * finalCoverage) / total));
            for (List<StackTraceElement> uniqueTrace : failureMap.keySet()) {
                Throwable t = new Throwable();
                t.setStackTrace(uniqueTrace.toArray(new StackTraceElement[0]));
                t.printStackTrace();
            }
        }
    }

    public void write(File coverageReportFile, File failuresReportFile) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(coverageReportFile)))) {
            out.printf("time, covered_branches (out of %d)%n", totalBranches);
            for (long[] row : rows) {
                out.printf("%d, %d%n", row[0], row[1]);
            }
        }
        try (PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(failuresReportFile)))) {
            int i = 1;
            for (List<StackTraceElement> uniqueTrace : failureMap.keySet()) {
                out.printf("%d. %s%n", i++, uniqueTrace);
                for (File inputFile : failureMap.get(uniqueTrace)) {
                    out.printf("\t%s%n", inputFile.toString());
                }
            }
        }
    }
}
