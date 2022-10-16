package edu.neu.ccs.prl.meringue;

import java.io.File;

public interface Replayer {
    void configure(String testClassName, String testMethodName, ClassLoader classLoader)
            throws ReflectiveOperationException;

    Throwable execute(byte[] input, File inputFile) throws Throwable;
}