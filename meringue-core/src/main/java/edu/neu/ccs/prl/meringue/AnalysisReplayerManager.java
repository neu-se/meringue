package edu.neu.ccs.prl.meringue;

import org.jacoco.agent.rt.RT;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public final class AnalysisReplayerManager implements Closeable, ReplayerManager {
    private final ForkConnection connection;
    private final StackTraceCleaner cleaner;
    private File nextInput = null;

    public AnalysisReplayerManager(int port, int maxTraceSize) throws IOException {
        this.connection = new ForkConnection(port);
        this.cleaner = new StackTraceCleaner(maxTraceSize);
    }

    @Override
    public File nextInput() {
        if (!hasNextInput()) {
            throw new IllegalStateException();
        }
        // Reset the JaCoCo coverage
        RT.getAgent().reset();
        File temp = nextInput;
        nextInput = null;
        return temp;
    }

    @Override
    public boolean hasNextInput() {
        if (nextInput == null) {
            try {
                nextInput = connection.receive(File.class);
            } catch (ClassNotFoundException e) {
                throw new AssertionError(e);
            } catch (IOException e) {
                return false;
            }
        }
        return nextInput != null;
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
