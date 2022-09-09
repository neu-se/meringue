package edu.berkeley.cs.jqf;

import edu.berkeley.cs.jqf.examples.ParameterizedFieldExample;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

import java.util.LinkedList;
import java.util.List;

public class ZestParameterizedRunnerTest {
    @Test
    public void junitAnnotationsRespected() throws Throwable {
        ParameterizedFieldExample.values.clear();
        new JUnitCore().run(ParameterizedFieldExample.class);
        List<String> expected = new LinkedList<>(ParameterizedFieldExample.values);
        Guidance guidance = new TestGuidance(2);
        ParameterizedFieldExample.values.clear();
        Runner runner = new ZestParameterizedRunner(ParameterizedFieldExample.class, "test1", guidance);
        runner.run(new RunNotifier());
        Assert.assertEquals(expected, ParameterizedFieldExample.values);
    }
}