package edu.neu.ccs.prl.meringue;

import com.code_intelligence.jazzer.replay.Replayer;

public final class JazzerReplayer implements edu.neu.ccs.prl.meringue.Replayer {
    @Override
    public void configure(String testClassName, String testMethodName, ClassLoader classLoader) {
        JazzerTarget.fuzzerInitialize(new String[]{testClassName, testMethodName});
        JazzerTarget.setRethrow(true);
    }

    @Override
    public Throwable execute(byte[] input) {
        Replayer.executeFuzzTarget(JazzerTarget.class, input);
        return JazzerTarget.getLastThrown();
    }
}
