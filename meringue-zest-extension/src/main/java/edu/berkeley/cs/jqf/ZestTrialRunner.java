package edu.berkeley.cs.jqf;

import org.junit.Test;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

public interface ZestTrialRunner {
    void runTrial(RunNotifier notifier, Object[] arguments) throws InitializationError;

    static FrameworkMethod getFrameworkMethod(TestClass testClass, String methodName) {
        return testClass.getAnnotatedMethods(Test.class).stream().filter(m -> m.getName().equals(methodName))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Unable to find test method"));
    }
}