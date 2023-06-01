package edu.neu.ccs.prl.meringue;

import java.io.File;

public class SimpleReplayManager implements ReplayerManager {
    private final File input;
    private boolean hasNext = true;

    public SimpleReplayManager(File input) {
        this.input = input;
    }

    @Override
    public File nextInput() {
        hasNext = false;
        return input;
    }

    @Override
    public boolean hasNextInput() {
        return hasNext;
    }

    @Override
    public void handleResult(Throwable failure) {
        if (failure != null) {
            failure.printStackTrace();
        }
    }

    @Override
    public void close() {
    }
}
