package edu.neu.ccs.prl.meringue.internal;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Produces cleaned stack traces. A cleaned stack trace is created by first identifying the root cause of an exception
 * or error. Non-included frames are removed from the stack trace for the root cause. Finally, the root cause trace
 * is trimmed to a specified maximum number of elements.
 */
public final class StackTraceCleaner implements Serializable {
    private static final String MERINGUE_INTERNAL_PACKAGE_PREFIX = "edu.neu.ccs.prl.meringue.internal";
    private static final long serialVersionUID = -8348499392963309412L;
    private final int maxSize;
    private final SerializablePredicate<StackTraceElement> includeFrame;

    public StackTraceCleaner(int maxSize, SerializablePredicate<StackTraceElement> includeFrame) {
        if (maxSize < 0) {
            throw new IllegalArgumentException();
        }
        if (includeFrame == null) {
            throw new NullPointerException();
        }
        this.maxSize = maxSize;
        this.includeFrame = includeFrame;
    }

    public StackTraceCleaner(int maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException();
        }
        this.maxSize = maxSize;
        this.includeFrame = (e) -> true;
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
            if (includeFrame.test(element) && !isInternalFrame(element)) {
                cleanedTrace.add(element);
            }
        }
        return cleanedTrace;
    }

    private static boolean isInternalFrame(StackTraceElement element) {
        return element.getClassName().startsWith(MERINGUE_INTERNAL_PACKAGE_PREFIX);
    }

    @FunctionalInterface
    public interface SerializablePredicate<T> extends Serializable, Predicate<T> {
    }
}
