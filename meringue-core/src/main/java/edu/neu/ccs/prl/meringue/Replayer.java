package edu.neu.ccs.prl.meringue;

import java.io.File;
import java.io.IOException;

public interface Replayer {
    void configure(String testClassName, String testMethodName, ClassLoader classLoader)
            throws ReflectiveOperationException, IOException;

    Throwable execute(byte[] input, File inputFile) throws Throwable;
}