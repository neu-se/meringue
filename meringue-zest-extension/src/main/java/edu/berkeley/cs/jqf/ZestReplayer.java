package edu.berkeley.cs.jqf;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import edu.neu.ccs.prl.meringue.FileUtil;
import edu.neu.ccs.prl.meringue.Replayer;
import org.junit.runners.model.MultipleFailureException;

import java.io.*;
import java.util.function.Consumer;

public final class ZestReplayer implements Replayer {
    private File argumentsDirectory;
    private Class<?> testClass;
    private String testMethodName;

    public ZestReplayer() {
        try {
            String argumentsDirectoryPath = System.getProperty("jqf.repro.dumpArgsDir");
            if (argumentsDirectoryPath != null) {
                argumentsDirectory = new File(argumentsDirectoryPath);
                FileUtil.createOrCleanDirectory(argumentsDirectory);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    public Throwable execute(byte[] input) {
        return execute(input, null);
    }

    @Override
    public Throwable execute(byte[] input, File inputFile) {
        ReplayGuidance guidance = new ReplayGuidance(input, inputFile, inputFile == null ? null : argumentsDirectory);
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
        private final File argumentsDirectory;
        private boolean consumed = false;
        private Throwable error = null;

        private ReplayGuidance(byte[] input, File inputFile, File argumentsDirectory) {
            this.input = input;
            this.inputFile = inputFile;
            this.argumentsDirectory = argumentsDirectory;
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
            if (argumentsDirectory != null) {
                try {
                    for (int i = 0; i < args.length; i++) {
                        File file = new File(argumentsDirectory, String.format("%s.%d", inputFile.getName(), i));
                        try (PrintWriter out = new PrintWriter(file)) {
                            out.print(args[i]);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
