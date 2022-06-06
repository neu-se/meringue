package edu.neu.ccs.prl.meringue;

import com.code_intelligence.jazzer.replay.Replayer;

public final class JazzerReplayer implements edu.neu.ccs.prl.meringue.Replayer {
    @Override
    public void configure(String testClassName, String testMethodName, ClassLoader classLoader) {
        JazzerTargetWrapper.fuzzerInitialize(new String[]{testClassName, testMethodName});
        JazzerTargetWrapper.setRethrow(false);
    }

    @Override
    public Throwable execute(byte[] input) {
        Replayer.executeFuzzTarget(JazzerTargetWrapper.class, input);
        return JazzerTargetWrapper.getLastThrown();
    }
}
