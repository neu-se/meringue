package edu.berkeley.cs.jqf;

import com.pholser.junit.quickcheck.generator.Generator;
import edu.berkeley.cs.jqf.examples.JUnitParamsExample;
import edu.berkeley.cs.jqf.examples.ParameterizedConstructorExample;
import edu.berkeley.cs.jqf.examples.ParameterizedFieldExample;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.model.TestClass;

import java.lang.reflect.Method;
import java.util.List;

public class ArgumentsGeneratorTest {
    @Test
    public void generatorsCorrectConstructorInjection() {
        TestClass clazz = new TestClass(ParameterizedConstructorExample.class);
        List<Generator<?>> generators = new ArgumentsGenerator(clazz.getOnlyConstructor(), 42).getGenerators();
        Assert.assertEquals(2, generators.size());
        Assert.assertTrue(generators.get(0).types().contains(int.class));
        Assert.assertTrue(generators.get(1).types().contains(long.class));
    }

    @Test
    public void generatorsCorrectFieldInjection() {
        TestClass clazz = new TestClass(ParameterizedFieldExample.class);
        List<Generator<?>> generators =
                new ArgumentsGenerator(ZestParameterizedRunner.getInjectableFields(clazz), 42).getGenerators();
        Assert.assertEquals(2, generators.size());
        Assert.assertTrue(generators.get(0).types().contains(String.class));
        Assert.assertTrue(generators.get(1).types().contains(int.class));
    }

    @Test
    public void generatorsCorrectMethodInjection() throws NoSuchMethodException {
        Method method = JUnitParamsExample.class.getDeclaredMethod("test1", int.class, boolean.class);
        List<Generator<?>> generators = new ArgumentsGenerator(method, 42).getGenerators();
        Assert.assertEquals(2, generators.size());
        Assert.assertTrue(generators.get(0).types().contains(int.class));
        Assert.assertTrue(generators.get(1).types().contains(boolean.class));
    }
}