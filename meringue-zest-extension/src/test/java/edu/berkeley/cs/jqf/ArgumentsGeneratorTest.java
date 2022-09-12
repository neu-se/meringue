package edu.berkeley.cs.jqf;

import com.pholser.junit.quickcheck.generator.Generator;
import edu.berkeley.cs.jqf.examples.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.model.TestClass;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    public void generatorsCorrectFieldInjectionFrom() {
        TestClass clazz = new TestClass(ParameterizedFieldFromExample.class);
        List<Generator<?>> generators =
                new ArgumentsGenerator(ZestParameterizedRunner.getInjectableFields(clazz), 42).getGenerators();
        Assert.assertEquals(Collections.singletonList(MyStringGenerator.class),
                            generators.stream().map(Object::getClass).collect(Collectors.toList()));
    }

    @Test
    public void generatorsCorrectMethodInjection() throws NoSuchMethodException {
        Method method = JUnitParamsExample.class.getDeclaredMethod("test1", int.class, boolean.class);
        List<Generator<?>> generators = new ArgumentsGenerator(method, 42).getGenerators();
        Assert.assertEquals(2, generators.size());
        Assert.assertTrue(generators.get(0).types().contains(int.class));
        Assert.assertTrue(generators.get(1).types().contains(boolean.class));
    }

    @Test
    public void generatorsCorrectMethodInjectionFrom() throws NoSuchMethodException {
        Method method = JUnitParamsExample.class.getDeclaredMethod("testWithGenerator", String.class);
        List<Generator<?>> generators = new ArgumentsGenerator(method, 42).getGenerators();
        Assert.assertEquals(Collections.singletonList(MyStringGenerator.class),
                            generators.stream().map(Object::getClass).collect(Collectors.toList()));
    }
}