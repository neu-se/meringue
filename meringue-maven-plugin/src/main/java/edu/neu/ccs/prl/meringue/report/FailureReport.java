package edu.neu.ccs.prl.meringue.report;

import edu.neu.ccs.prl.meringue.Failure;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class FailureReport {
    private final Map<Failure, FailureEntry> failureMap = new LinkedHashMap<>();
    private final long firstTimestamp;

    public FailureReport(long firstTimestamp) {
        this.firstTimestamp = firstTimestamp;
    }

    public void record(File inputFile, Failure failure) {
        if (failure != null) {
            long time = inputFile.lastModified() - firstTimestamp;
            if (!failureMap.containsKey(failure)) {
                failureMap.put(failure, new FailureEntry(time, failure));
            }
            failureMap.get(failure).inducingInputs.add(inputFile);
        }
    }

    public void write(File file) throws IOException {
        ReportUtil.writeJsonList(file, FailureEntry.class, new LinkedList<>(failureMap.values()));
    }

    public int getNumberOfUniqueFailures() {
        return failureMap.size();
    }

    /**
     * Record type used for JSON reports.
     */
    @SuppressWarnings({"unused", "FieldCanBeLocal", "MismatchedQueryAndUpdateOfCollection"})
    private static final class FailureEntry {
        private final long time;
        private final Failure failure;
        private final List<File> inducingInputs = new LinkedList<>();

        private FailureEntry(long time, Failure failure) {
            this.time = time;
            this.failure = failure;
        }
    }
}
