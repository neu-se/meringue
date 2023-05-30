package edu.neu.ccs.prl.meringue;

import org.jacoco.agent.rt.RT;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public final class AnalysisReplayerManager implements Closeable, ReplayerManager {
    private final ForkConnection connection;
    private final StackTraceCleaner cleaner;

    public AnalysisReplayerManager(int port, int maxTraceSize) throws IOException {
        this.connection = new ForkConnection(port);
        this.cleaner = new StackTraceCleaner(maxTraceSize);
    }

    @Override
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

    @Override
    public boolean hasNextInput() {
        return !connection.isClosed();
    }

    @Override
    public void handleResult(Throwable failure) throws IOException {
        // Send current JaCoCo coverage
        connection.send(RT.getAgent().getExecutionData(false));
        // Send the failure
        if (failure == null) {
            connection.send(false);
        } else {
            Throwable rootCause = cleaner.getRootCause(failure);
            StackTraceElement[] trace = cleaner.cleanStackTrace(rootCause).toArray(new StackTraceElement[0]);
            connection.send(true);
            connection.send(new Failure(rootCause.getClass().getName(), trace));
            connection.send(rootCause.getMessage());
        }
    }

    @Override
    public void close() {
        connection.close();
    }
}
