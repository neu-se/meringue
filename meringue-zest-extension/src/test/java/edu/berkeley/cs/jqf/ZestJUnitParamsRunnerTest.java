package edu.berkeley.cs.jqf;

import edu.berkeley.cs.jqf.examples.JUnitParamsExample;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

import java.util.LinkedList;
import java.util.List;

public class ZestJUnitParamsRunnerTest {
    @Test
    public void junitAnnotationsRespected() throws Throwable {
        JUnitParamsExample.values.clear();
        new JUnitCore().run(JUnitParamsExample.class);
        List<String> expected = new LinkedList<>(JUnitParamsExample.values);
        Guidance guidance = new TestGuidance(2);
        JUnitParamsExample.values.clear();
        Runner runner = new ZestJUnitParamsRunner(JUnitParamsExample.class, "test1", guidance);
        runner.run(new RunNotifier());
        Assert.assertEquals(expected, JUnitParamsExample.values);
    }
}