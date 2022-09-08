package edu.berkeley.cs.jqf;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.instrument.tracing.SingleSnoop;
import edu.berkeley.cs.jqf.instrument.tracing.TraceLogger;
import org.junit.Test;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import junitparams.internal.TestMethod;

import junitparams.JUnitParamsRunner;

public class JUnitParamsZestRunner extends JUnitParamsRunner{
    private final Guidance guidance;
    private final FrameworkMethod method;

    public JUnitParamsZestRunner(Class<?> clazz, String methodName, Guidance guidance) throws Throwable {
        super(clazz);
        this.guidance = guidance;
        this.method = getFrameworkMethod(getTestClass(), methodName);
    }

    @Override
    protected Statement childrenInvoker(final RunNotifier notifier) {
        return new ParameterizedZestStatement(getTestClass(), method, guidance);
    }

    /*
    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        /*
        if (handleIgnored(method, notifier))
            return;

        TestMethod testMethod = parameterisedRunner.testMethodFor(method);
        if (testMethod.isIgnored())
            return;
        if (parameterisedRunner.shouldRun(testMethod)) {
            //Statement statement = methodBlock(method);
            Statement statement = childrenInvoker(notifier);
            parameterisedRunner.runParameterisedTest(testMethod, statement, notifier);
        } else {
            verifyMethodCanBeRunByStandardRunner(testMethod);
            super.runChild(method, notifier);
        }
    }
    */

    @Override
    protected Statement methodBlock(final FrameworkMethod method) {
        return new ParameterizedZestStatement(getTestClass(), method, guidance);
    }

    static FrameworkMethod getFrameworkMethod(TestClass clazz, String methodName) {
        return clazz.getAnnotatedMethods(Test.class).stream().filter(m -> m.getName().equals(methodName)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unable to find test method"));
    }

    static JUnitParamsZestRunner create(Class<?> clazz, String methodName, Guidance guidance) {
        try {
            return new JUnitParamsZestRunner(clazz, methodName, guidance);
        } catch (Throwable e) {
            throw new IllegalArgumentException("Unable to create JUnitParams runner for test: " + clazz, e);
        }
    }

    static void run(Class<?> clazz, String methodName, Guidance guidance) {
        try {
            JUnitParamsZestRunner runner = create(clazz, methodName, guidance);
            SingleSnoop.setCallbackGenerator(guidance::generateCallBack);
            SingleSnoop.startSnooping(clazz.getName() + "#" + methodName);
            runner.run(new RunNotifier());
        } finally {
            TraceLogger.get().remove();
        }
    }
}
