package edu.neu.ccs.prl.meringue.report;

import edu.neu.ccs.prl.meringue.CoverageCalculator;
import edu.neu.ccs.prl.meringue.JacocoReportFormat;
import org.jacoco.core.tools.ExecFileLoader;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class CoverageReport {
    private final CoverageCalculator calculator;
    private final List<long[]> rows = new ArrayList<>();
    private final long firstTimestamp;
    private byte[] lastExecData = null;

    public CoverageReport(CoverageCalculator calculator, long firstTimestamp) {
        this.calculator = calculator;
        this.firstTimestamp = firstTimestamp;
    }

    public void write(File file) throws IOException {
        try (PrintStream out = new PrintStream(new BufferedOutputStream(Files.newOutputStream(file.toPath())))) {
            out.printf("time, covered_branches%n");
            for (long[] row : rows) {
                out.printf("%d, %d%n", row[0], row[1]);
            }
        }
    }

    public long getCoveredBranches() {
        return rows.isEmpty() ? 0 : rows.get(rows.size() - 1)[1];
    }

    public long getTotalBranches() {
        return calculator.getTotalBranches();
    }

    public void writeJacocoReports(String testDescription, File directory, Iterable<JacocoReportFormat> formats)
            throws IOException {
        for (JacocoReportFormat format : formats) {
            calculator.createReport(lastExecData, testDescription, format, directory);
        }
    }

    public void record(File inputFile, byte[] execData) throws IOException {
        long time = inputFile.lastModified() - firstTimestamp;
        lastExecData = (lastExecData == null) ? execData : mergeExecData(lastExecData, execData);
        long coverage = calculator.calculate(lastExecData);
        if (rows.isEmpty() || coverage > rows.get(rows.size() - 1)[1]) {
            rows.add(new long[]{time, coverage});
        }
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
