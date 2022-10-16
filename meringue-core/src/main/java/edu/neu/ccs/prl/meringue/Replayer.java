package edu.neu.ccs.prl.meringue;

import java.io.File;
import java.io.IOException;

public interface Replayer {
    void configure(String testClassName, String testMethodName, ClassLoader classLoader)
            throws ReflectiveOperationException, IOException;

    Throwable execute(File input) throws IOException;

    default void accept(ReplayerManager manager) throws IOException {
        while (manager.hasNextInput()) {
            File input = manager.nextInput();
            Throwable failure = execute(input);
            manager.handleResult(failure);
        }
    }
}