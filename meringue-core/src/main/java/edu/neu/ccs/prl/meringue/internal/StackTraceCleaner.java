package edu.neu.ccs.prl.meringue.internal;

import java.util.LinkedList;
import java.util.List;

/**
 * Produces cleaned stack traces. A cleaned stack trace is created by first identifying the root cause of an exception
 * or error. Internal meringue frames are removed from the stack trace for the root cause.
 * Finally, the root cause trace is trimmed to a specified maximum number of elements.
 */
public class StackTraceCleaner {
    private static final String MERINGUE_INTERNAL_PACKAGE_PREFIX = "edu.neu.ccs.prl.meringue.internal";
    private final int maxSize;

    public StackTraceCleaner(int maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException();
        }
        this.maxSize = maxSize;
    }

    public List<StackTraceElement> cleanStackTrace(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        List<StackTraceElement> cleanedTrace = new LinkedList<>();
        for (StackTraceElement element : t.getStackTrace()) {
            if (cleanedTrace.size() == maxSize) {
                return cleanedTrace;
            }
            if (!isInternalFrame(element)) {
                cleanedTrace.add(element);
            }
        }
        return cleanedTrace;
    }

    protected boolean isInternalFrame(StackTraceElement element) {
        return element.getClassName().startsWith(MERINGUE_INTERNAL_PACKAGE_PREFIX);
    }
}
