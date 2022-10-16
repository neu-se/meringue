package edu.neu.ccs.prl.meringue;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.function.Consumer;

public final class ZestReplayer implements Replayer {
    private Class<?> testClass;
    private String testMethodName;

    @Override
    public void configure(String testClassName, String testMethodName, ClassLoader classLoader)
            throws ClassNotFoundException {
        if (testClassName == null || testMethodName == null) {
            throw new NullPointerException();
        }
        this.testMethodName = testMethodName;
        this.testClass = Class.forName(testClassName, true, classLoader);
    }

    private Throwable execute(File input) throws IOException {
        ReplayGuidance guidance = new ReplayGuidance(Files.readAllBytes(input.toPath()));
        GuidedFuzzing.run(testClass, testMethodName, guidance, System.out);
        return guidance.error;
    }

    @Override
    public void accept(ReplayerManager manager) throws IOException {
        while (manager.hasNextInput()) {
            File input = manager.nextInput();
            Throwable failure = execute(input);
            manager.handleResult(failure);
        }
    }

    private static final class ReplayGuidance implements Guidance {
        private final byte[] input;
        private boolean consumed = false;
        private Throwable error = null;

        private ReplayGuidance(byte[] input) {
            this.input = input;
        }

        @Override
        public InputStream getInput() throws IllegalStateException, GuidanceException {
            return new ByteArrayInputStream(input);
        }

        @Override
        public boolean hasInput() {
            boolean result = !consumed;
            consumed = true;
            return result;
        }

        @Override
        public void handleResult(Result result, Throwable error) throws GuidanceException {
            if (result == Result.FAILURE) {
                this.error = error;
            }
        }

        @Override
        public Consumer<TraceEvent> generateCallBack(Thread thread) {
            return (t) -> {
            };
        }
    }
}
