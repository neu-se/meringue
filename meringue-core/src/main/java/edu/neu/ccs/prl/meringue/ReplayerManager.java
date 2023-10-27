package edu.neu.ccs.prl.meringue;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public interface ReplayerManager extends Closeable {
    File nextInput() throws IOException;

    boolean hasNextInput() throws IOException;

    void handleResult(Throwable failure) throws IOException;
}
