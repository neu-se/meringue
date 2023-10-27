package edu.neu.ccs.prl.meringue;

import com.code_intelligence.jazzer.driver.Opt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public final class JazzerReplayer implements edu.neu.ccs.prl.meringue.Replayer {
    private FuzzTargetRunner runner;

    @Override
    public void configure(String testClassName, String testMethodName, ClassLoader classLoader) throws Throwable {
        if ("fuzzerTestOneInput".equals(testMethodName)) {
            runner = new FuzzTargetRunner(testClassName);
        } else {
            Opt.targetArgs.setIfDefault(Arrays.asList(testClassName, testMethodName));
            runner = new FuzzTargetRunner(JazzerTargetWrapper.class.getName());
        }
    }

    private Throwable execute(File input) throws IOException {
        return runner.run(Files.readAllBytes(input.toPath()));
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
