package edu.berkeley.cs.jqf;

import edu.berkeley.cs.jqf.examples.ParameterizedConstructorExample;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

public class ZestTrialRunnerTest {
    @Test
    public void frameworkMethodFound() {
        FrameworkMethod method =
                ZestTrialRunner.getFrameworkMethod(new TestClass(ParameterizedConstructorExample.class), "test1");
        Assert.assertNotNull(method);
        Assert.assertEquals("test1", method.getName());
    }
}