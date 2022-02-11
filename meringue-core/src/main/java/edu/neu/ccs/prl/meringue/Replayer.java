package edu.neu.ccs.prl.meringue;

public interface Replayer {
    void configure(String testClassName, String testMethodName, ClassLoader classLoader)
            throws ReflectiveOperationException;

    Throwable execute(byte[] input);
}