package edu.neu.ccs.prl.meringue;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Produces cleaned stack traces. A cleaned stack trace is created by first identifying the root cause of an exception
 * or error. Internal frames are removed from the stack trace for the root cause. Finally, the root cause trace is
 * trimmed to a specified maximum number of elements.
 */
public class StackTraceCleaner {
    private final int maxSize;
    private final Predicate<StackTraceElement> isInternal;

    public StackTraceCleaner(int maxSize) {
        this(maxSize, (e) -> false);
    }

    public StackTraceCleaner(int maxSize, Predicate<StackTraceElement> isInternal) {
        if (maxSize < 0) {
            throw new IllegalArgumentException();
        }
        if (isInternal == null) {
            throw new NullPointerException();
        }
        this.maxSize = maxSize;
        this.isInternal = isInternal;
    }

    public List<StackTraceElement> cleanStackTrace(Throwable t) {
        t = getRootCause(t);
        List<StackTraceElement> cleanedTrace = new LinkedList<>();
        for (StackTraceElement element : t.getStackTrace()) {
            if (cleanedTrace.size() == maxSize) {
                return cleanedTrace;
            }
            if (!isInternal.test(element)) {
                cleanedTrace.add(element);
            }
        }
        return cleanedTrace;
    }

    public Throwable getRootCause(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }
}
