package edu.neu.ccs.prl.meringue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Failure implements Serializable {
    private static final long serialVersionUID = -5292161279633260125L;
    private final String type;
    private final List<StackTraceElement> trace;

    public Failure(String type, StackTraceElement[] trace) {
        if (type == null) {
            throw new NullPointerException();
        }
        this.type = type;
        this.trace = Collections.unmodifiableList(Arrays.asList(trace));
    }

    public String getType() {
        return type;
    }

    public List<StackTraceElement> getTrace() {
        return trace;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + trace.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Failure)) {
            return false;
        }
        Failure failure = (Failure) o;
        if (!type.equals(failure.type)) {
            return false;
        }
        return trace.equals(failure.trace);
    }

    @Override
    public String toString() {
        return type + ": " + trace;
    }
}
