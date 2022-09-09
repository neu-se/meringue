package edu.berkeley.cs.jqf;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized;
import org.junit.runners.model.*;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.TestWithParameters;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ZestParameterizedRunner extends Parameterized implements ZestTrialRunner {
    private static final long SEED = 42;
    private final Guidance guidance;
    private final FrameworkMethod method;

    public ZestParameterizedRunner(Class<?> clazz, String methodName, Guidance guidance) throws Throwable {
        super(clazz);
        this.guidance = guidance;
        this.method = ZestTrialRunner.getFrameworkMethod(getTestClass(), methodName);
    }

    @Override
    protected Statement childrenInvoker(RunNotifier notifier) {
        ArgumentsGenerator generator;
        if (getTestClass().getAnnotatedFields(Parameterized.Parameter.class).isEmpty()) {
            generator = new ArgumentsGenerator(getTestClass().getOnlyConstructor(), SEED);
        } else {
            generator = new ArgumentsGenerator(getInjectableFields(getTestClass()), SEED);
        }
        return new ZestStatement(guidance, generator, this);
    }

    @Override
    public void runTrial(RunNotifier notifier, Object[] arguments) throws InitializationError {
        new TrialRunner(getTestClass(), method, arguments).run(notifier);
    }

    static List<Field> getInjectableFields(TestClass clazz) {
        return clazz.getAnnotatedFields(Parameterized.Parameter.class).stream().map(FrameworkField::getField)
                    .sorted(Comparator.comparing(f -> f.getAnnotation(Parameterized.Parameter.class).value()))
                    .collect(Collectors.toList());
    }

    private static class TrialRunner extends BlockJUnit4ClassRunnerWithParameters {
        private final FrameworkMethod method;

        TrialRunner(TestClass testClass, FrameworkMethod method, Object[] arguments) throws InitializationError {
            super(new TestWithParameters("[0]", testClass, Arrays.asList(arguments)));
            this.method = method;
        }

        @Override
        protected void runChild(FrameworkMethod method, RunNotifier notifier) {
            if (method.equals(this.method)) {
                super.runChild(method, notifier);
            }
        }
    }
}
