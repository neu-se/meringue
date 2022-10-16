package edu.neu.ccs.prl.meringue;

import org.jacoco.agent.rt.RT;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class ReplayerManager implements Closeable {
    private final ForkConnection connection;
    private final StackTraceCleaner cleaner;

    public ReplayerManager(int port, int maxTraceSize) throws IOException {
        this.connection = new ForkConnection(port);
        this.cleaner = new StackTraceCleaner(maxTraceSize);
    }

    public File nextInput() throws IOException {
        File input;
        try {
            input = connection.receive(File.class);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
        // Reset the JaCoCo coverage
        RT.getAgent().reset();
        return input;
    }

    public boolean hasNextInput() {
        return !connection.isClosed();
    }

    public void handleResult(Throwable failure) throws IOException {
        // Send current JaCoCo coverage
        connection.send(RT.getAgent().getExecutionData(false));
        // Send the failure
        if (failure == null) {
            connection.send(false);
        } else {
            connection.send(true);
            connection.send(cleaner.cleanStackTrace(failure).toArray(new StackTraceElement[0]));
        }
    }

    @Override
    public void close() {
        connection.close();
    }
}
