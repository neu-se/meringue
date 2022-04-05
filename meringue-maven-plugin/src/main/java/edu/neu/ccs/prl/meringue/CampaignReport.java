package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.logging.Log;
import org.jacoco.core.tools.ExecFileLoader;

import java.io.*;
import java.util.*;

final class CampaignReport {
    private final Map<List<StackTraceElement>, List<File>> failureMap = new HashMap<>();
    private final long totalBranches;
    private final CoverageCalculator calculator;
    private final List<long[]> rows = new ArrayList<>();
    private final File[] sources;
    private long firstTimestamp = -1;
    private byte[] lastExecData = null;

    public CampaignReport(CoverageCalculator calculator, File[] sources) {
        this.totalBranches = calculator.getTotalBranches();
        this.calculator = calculator;
        this.sources = sources.clone();
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

    public void write(File coverageFile, File failuresFile) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(coverageFile)))) {
            out.printf("time, covered_branches (out of %d)%n", totalBranches);
            for (long[] row : rows) {
                out.printf("%d, %d%n", row[0], row[1]);
            }
        }
        try (PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(failuresFile)))) {
            int i = 1;
            for (List<StackTraceElement> uniqueTrace : failureMap.keySet()) {
                out.printf("%d. %s%n", i++, uniqueTrace);
                for (File inputFile : failureMap.get(uniqueTrace)) {
                    out.printf("\t%s%n", inputFile.toString());
                }
            }
        }
    }

    public void writeHtmlReport(String testDescription, File reportDir) throws IOException {
        calculator.createHtmlReport(lastExecData == null ? new byte[0] : lastExecData, testDescription, sources,
                reportDir);
    }

    public void record(File inputFile, byte[] execData, StackTraceElement[] trace) throws IOException {
        if (firstTimestamp == -1) {
            firstTimestamp = inputFile.lastModified();
        }
        long time = inputFile.lastModified() - firstTimestamp;
        lastExecData = (lastExecData == null) ? execData : mergeExecData(lastExecData, execData);
        rows.add(new long[]{time, calculator.calculate(lastExecData)});
        if (trace != null) {
            List<StackTraceElement> elements = Arrays.asList(trace);
            if (!failureMap.containsKey(elements)) {
                failureMap.put(elements, new ArrayList<>());
            }
            failureMap.get(elements).add(inputFile);
        }
    }

    public void recordJvmCrash(File inputFile) {
    }

    private static byte[] mergeExecData(byte[] execData1, byte[] execData2) throws IOException {
        ExecFileLoader loader = new ExecFileLoader();
        loader.load(new ByteArrayInputStream(execData1));
        loader.load(new ByteArrayInputStream(execData2));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        loader.save(out);
        return out.toByteArray();
    }
}
