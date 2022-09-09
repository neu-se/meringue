package edu.berkeley.cs.jqf;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

import java.io.InputStream;
import java.util.function.Consumer;

final class TestGuidance implements Guidance {
    private int numberOfInputs;

    TestGuidance(int numberOfInputs) {
        this.numberOfInputs = numberOfInputs;
    }

    @Override
    public InputStream getInput() throws IllegalStateException, GuidanceException {
        return new ZeroStream();
    }

    @Override
    public boolean hasInput() {
        return numberOfInputs-- > 0;
    }

    @Override
    public void handleResult(Result result, Throwable error) throws GuidanceException {
    }

    @Override
    public Consumer<TraceEvent> generateCallBack(Thread thread) {
        return (t) -> {
        };
    }

    private static final class ZeroStream extends InputStream {
        @Override
        public int read() {
            return 0;
        }
    }
}
