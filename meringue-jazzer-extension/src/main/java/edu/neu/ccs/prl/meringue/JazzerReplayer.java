package edu.neu.ccs.prl.meringue;

import com.code_intelligence.jazzer.replay.Replayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class JazzerReplayer implements edu.neu.ccs.prl.meringue.Replayer {
    @Override
    public void configure(String testClassName, String testMethodName, ClassLoader classLoader) {
        JazzerTargetWrapper.fuzzerInitialize(new String[]{testClassName, testMethodName});
        JazzerTargetWrapper.setRethrow(false);
    }

    private Throwable execute(File input) throws IOException {
        Replayer.executeFuzzTarget(JazzerTargetWrapper.class, Files.readAllBytes(input.toPath()));
        return JazzerTargetWrapper.getLastThrown();
    }

    @Override
    public void accept(ReplayerManager manager) throws IOException {
        while (manager.hasNextInput()) {
            File input = manager.nextInput();
            Throwable failure = execute(input);
            manager.handleResult(failure);
        }
    }
}
