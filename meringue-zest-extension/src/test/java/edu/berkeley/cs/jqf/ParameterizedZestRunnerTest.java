package edu.berkeley.cs.jqf;

import com.pholser.junit.quickcheck.generator.Generator;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class ParameterizedZestRunnerTest {
    @Test
    public void frameworkMethodFound() {
        FrameworkMethod method =
                ParameterizedZestRunner.getFrameworkMethod(new TestClass(ConstructorParamExample.class), "test1");
        Assert.assertNotNull(method);
        Assert.assertEquals("test1", method.getName());
    }

    @Test
    public void generatorsCorrectConstructorInjection() {
        TestClass clazz = new TestClass(ConstructorParamExample.class);
        List<Generator<?>> generators = ParameterizedZestStatement.createGenerators(clazz);
        Assert.assertEquals(2, generators.size());
        Assert.assertTrue(generators.get(0).types().contains(int.class));
        Assert.assertTrue(generators.get(1).types().contains(long.class));
    }

    @Test
    public void generatorsCorrectFieldInjection() {
        TestClass clazz = new TestClass(FieldParamExample.class);
        List<Generator<?>> generators = ParameterizedZestStatement.createGenerators(clazz);
        Assert.assertEquals(2, generators.size());
        Assert.assertTrue(generators.get(0).types().contains(String.class));
        Assert.assertTrue(generators.get(1).types().contains(int.class));
    }

    @Test
    public void junitAnnotationsAppliedCorrectlyExecution() {
        FieldParamExample.values.clear();
        new JUnitCore().run(FieldParamExample.class);
        List<String> expected = new LinkedList<>(FieldParamExample.values);
        Guidance guidance = new TestGuidance(2);
        FieldParamExample.values.clear();
        ParameterizedZestRunner runner = ParameterizedZestRunner.create(FieldParamExample.class, "test1", guidance);
        runner.run(new RunNotifier());
        Assert.assertEquals(expected, FieldParamExample.values);
    }

    private static final class TestGuidance implements Guidance {
        private int count;

        private TestGuidance(int count) {
            this.count = count;
        }

        @Override
        public InputStream getInput() throws IllegalStateException, GuidanceException {
            return new ZeroStream();
        }

        @Override
        public boolean hasInput() {
            return count-- > 0;
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
}
