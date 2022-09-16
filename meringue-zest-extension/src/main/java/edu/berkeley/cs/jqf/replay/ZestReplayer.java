package edu.berkeley.cs.jqf.replay;

import edu.berkeley.cs.jqf.ZestForkMain;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import edu.neu.ccs.prl.meringue.Replayer;
import org.junit.runners.model.MultipleFailureException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public final class ZestReplayer implements Replayer {
    private final ArgumentsWriter writer;
    private Class<?> testClass;
    private String testMethodName;

    public ZestReplayer() throws IOException {
        String path = System.getProperty("zest.argumentsDir");
        writer = path == null ? (a, f) -> {
        } : new FileArgumentsWriter(new File(path));
    }

    @Override
    public void configure(String testClassName, String testMethodName, ClassLoader classLoader)
            throws ClassNotFoundException {
        if (testClassName == null || testMethodName == null) {
            throw new NullPointerException();
        }
        this.testMethodName = testMethodName;
        this.testClass = Class.forName(testClassName, true, classLoader);
    }

    @Override
    public Throwable execute(byte[] input, File inputFile) {
        ReplayGuidance guidance = new ReplayGuidance(input, inputFile, writer);
        try {
            ZestForkMain.run(testClass, testMethodName, guidance);
        } catch (MultipleFailureException e) {
            // Suppress test failure
        }
        return guidance.error;
    }

    private static final class ReplayGuidance implements Guidance {
        private final byte[] input;
        private final File inputFile;
        private final ArgumentsWriter writer;
        private boolean consumed = false;
        private Throwable error = null;

        private ReplayGuidance(byte[] input, File inputFile, ArgumentsWriter writer) {
            this.input = input;
            this.inputFile = inputFile;
            this.writer = writer;
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
        public void observeGeneratedArgs(Object[] args) {
            try {
                writer.write(args, inputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
