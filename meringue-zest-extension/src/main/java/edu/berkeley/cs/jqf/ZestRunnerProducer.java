package edu.berkeley.cs.jqf;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import org.junit.runner.Runner;

public interface ZestRunnerProducer {
    Runner produce(Class<?> clazz, String methodName, Guidance guidance) throws Throwable;
}
