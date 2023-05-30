package edu.neu.ccs.prl.meringue;

import java.io.File;

public final class ReplayForkMain {
    private ReplayForkMain() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws Throwable {
        String testClassName = args[0];
        String testMethodName = args[1];
        String replayerClassName = args[2];
        File input = new File(args[3]);
        Replayer replayer = (Replayer) Class.forName(replayerClassName).getDeclaredConstructor().newInstance();
        replayer.configure(testClassName, testMethodName, ReplayForkMain.class.getClassLoader());
        try (ReplayerManager manager = new SimpleReplayManager(input)) {
            replayer.accept(manager);
        }
    }
}