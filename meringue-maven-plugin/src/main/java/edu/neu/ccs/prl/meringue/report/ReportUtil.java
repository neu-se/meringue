package edu.neu.ccs.prl.meringue.report;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.ToJson;
import com.squareup.moshi.Types;
import okio.BufferedSink;
import okio.Okio;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

public final class ReportUtil {
    private static final Moshi MOSHI = new Moshi.Builder()
            .add(new ReportAdapter())
            .build();

    private ReportUtil() {
        throw new AssertionError();
    }

    public static <T> void writeJson(File file, Class<T> clazz, T value) throws IOException {
        try (BufferedSink sink = Okio.buffer(Okio.sink(file))) {
            MOSHI.adapter(clazz)
                 .indent("    ")
                 .toJson(sink, value);
        }
    }

    public static <T> void writeJsonList(File file, Class<T> clazz, List<T> values) throws IOException {
        try (BufferedSink sink = Okio.buffer(Okio.sink(file))) {
            MOSHI.adapter(Types.newParameterizedType(List.class, clazz))
                 .indent("    ")
                 .toJson(sink, values);
        }
    }

    @SuppressWarnings("unused")
    public static final class ReportAdapter {
        @ToJson
        String toPath(File file) {
            return file.getAbsolutePath();
        }

        @FromJson
        File fromPath(String pathName) {
            return new File(pathName);
        }

        @ToJson
        long toMillis(Duration duration) {
            return duration.toMillis();
        }

        @FromJson
        Duration fromMillis(long milliseconds) {
            return Duration.ofMillis(milliseconds);
        }

        @ToJson
        StackTraceElementRecord toRecord(StackTraceElement element) {
            return new StackTraceElementRecord(element);
        }

        @FromJson
        StackTraceElement fromRecord(StackTraceElementRecord record) {
            return record.toElement();
        }
    }

    public static final class StackTraceElementRecord {
        private final String declaringClass;
        private final String methodName;
        private final String fileName;
        private final int lineNumber;

        public StackTraceElementRecord(StackTraceElement element) {
            this.declaringClass = element.getClassName();
            this.methodName = element.getMethodName();
            this.fileName = element.getFileName();
            this.lineNumber = element.getLineNumber();
        }

        public StackTraceElement toElement() {
            return new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
        }
    }
}
