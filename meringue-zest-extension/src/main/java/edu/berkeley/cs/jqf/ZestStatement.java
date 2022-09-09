package edu.berkeley.cs.jqf;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.guidance.TimeoutException;
import edu.berkeley.cs.jqf.instrument.InstrumentationException;
import org.junit.AssumptionViolatedException;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import java.util.*;

import static edu.berkeley.cs.jqf.fuzz.guidance.Result.*;

class ZestStatement extends Statement {
    private final List<Throwable> failures = new LinkedList<>();
    private final Set<List<StackTraceElement>> traces = new HashSet<>();
    private final Guidance guidance;
    private final ArgumentsGenerator generator;
    private final ZestTrialRunner runner;

    public ZestStatement(Guidance guidance, ArgumentsGenerator generator, ZestTrialRunner runner) {
        if (guidance == null || generator == null || runner == null) {
            throw new NullPointerException();
        }
        this.guidance = guidance;
        this.generator = generator;
        this.runner = runner;
    }

    public void evaluate() throws MultipleFailureException, InitializationError {
        while (guidance.hasInput()) {
            Result result = SUCCESS;
            Throwable error = null;
            try {
                Object[] arguments = generator.generate(guidance);
                guidance.observeGeneratedArgs(arguments);
                ZestNotifier notifier = new ZestNotifier();
                runner.runTrial(notifier, arguments);
                if (notifier.failure != null) {
                    throw notifier.failure;
                }
            } catch (InitializationError | GuidanceException e) {
                throw e;
            } catch (InstrumentationException e) {
                throw new GuidanceException(e);
            } catch (AssumptionViolatedException e) {
                result = INVALID;
                error = e;
            } catch (TimeoutException e) {
                result = TIMEOUT;
                error = e;
            } catch (Throwable e) {
                result = FAILURE;
                error = e;
                if (traces.add(Arrays.asList(e.getStackTrace()))) {
                    failures.add(e);
                }
            }
            guidance.handleResult(result, error);
        }
        if (!failures.isEmpty()) {
            throw new MultipleFailureException(failures);
        }
    }

    private static final class ZestNotifier extends RunNotifier {
        Throwable failure;

        @Override
        public void fireTestFailure(Failure failure) {
            this.failure = failure.getException();
        }

        @Override
        public void fireTestAssumptionFailed(Failure failure) {
            this.failure = failure.getException();
        }
    }
}
