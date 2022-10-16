package edu.neu.ccs.prl.meringue;

import java.io.IOException;

public interface Replayer {
    void configure(String testClassName, String testMethodName, ClassLoader classLoader)
            throws ReflectiveOperationException, IOException;

    void accept(ReplayerManager manager) throws IOException;
}