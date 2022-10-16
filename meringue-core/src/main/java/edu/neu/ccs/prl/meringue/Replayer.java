package edu.neu.ccs.prl.meringue;

public interface Replayer {
    void configure(String testClassName, String testMethodName, ClassLoader classLoader)
            throws Throwable;

    void accept(ReplayerManager manager) throws Throwable;
}