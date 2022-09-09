package edu.berkeley.cs.jqf;

import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.berkeley.cs.jqf.instrument.tracing.SingleSnoop;
import edu.berkeley.cs.jqf.instrument.tracing.TraceLogger;
import junitparams.JUnitParamsRunner;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized;
import org.junit.runners.model.MultipleFailureException;

import java.io.File;

public final class ZestForkMain {
    private ZestForkMain() {
        throw new AssertionError(
                getClass().getSimpleName() + " is a static utility class and should not be instantiated");
    }

    public static void main(String[] args) throws Throwable {
        String testClassName = args[0];
        String testMethodName = args[1];
        File outputDir = new File(args[2]);
        Guidance guidance = new ZestGuidance(testClassName + "#" + testMethodName, null, outputDir);
        ClassLoader testClassLoader = ZestForkMain.class.getClassLoader();
        Class<?> testClass = java.lang.Class.forName(testClassName, true, testClassLoader);
        run(testClass, testMethodName, guidance);
    }

    static void run(Class<?> testClass, String testMethodName, Guidance guidance) throws MultipleFailureException {
        Class<? extends Runner> runnerClass = getRunnerClass(testClass);
        if (runnerClass.equals(JQF.class)) {
            GuidedFuzzing.run(testClass, testMethodName, guidance, System.out);
        } else if (runnerClass.equals(Parameterized.class)) {
            run(ZestParameterizedRunner::new, testClass, testMethodName, guidance);
        } else if (runnerClass.equals(JUnitParamsRunner.class)) {
            run(ZestJUnitParamsRunner::new, testClass, testMethodName, guidance);
        } else {
            throw new IllegalArgumentException("Unknown test class runner type:" + runnerClass);
        }
    }

    static Class<? extends Runner> getRunnerClass(Class<?> testClass) {
        if (!testClass.isAnnotationPresent(RunWith.class)) {
            throw new IllegalArgumentException("Test class must be annotated with RunWith: " + testClass);
        }
        RunWith annotation = testClass.getAnnotation(RunWith.class);
        return annotation.value();
    }

    static void run(ZestRunnerProducer builder, Class<?> clazz, String methodName, Guidance guidance) {
        try {
            Runner runner = build(builder, clazz, methodName, guidance);
            SingleSnoop.setCallbackGenerator(guidance::generateCallBack);
            SingleSnoop.startSnooping(clazz.getName() + "#" + methodName);
            runner.run(new RunNotifier());
        } finally {
            TraceLogger.get().remove();
        }
    }

    static Runner build(ZestRunnerProducer builder, Class<?> clazz, String methodName, Guidance guidance) {
        try {
            return builder.produce(clazz, methodName, guidance);
        } catch (Throwable e) {
            throw new IllegalArgumentException("Unable to create JUnit runner for test: " + clazz + " " + methodName,
                                               e);
        }
    }
}