package edu.neu.ccs.prl.meringue.internal;

public interface Replayer {
    void configure(String testClassName, String testMethodName, ClassLoader classLoader);

    Throwable execute(byte[] input);
}