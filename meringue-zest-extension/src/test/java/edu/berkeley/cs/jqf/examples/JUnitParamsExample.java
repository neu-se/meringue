package edu.berkeley.cs.jqf.examples;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.*;
import org.junit.runner.RunWith;

import java.util.LinkedList;
import java.util.List;

@RunWith(JUnitParamsRunner.class)
@SuppressWarnings("all")
public class JUnitParamsExample {
    public static final List<String> values = new LinkedList<>();

    @Before
    public void before() {
        values.add("b");
    }

    @After
    public void after() {
        values.add("a");
    }

    @Test
    @Parameters({"77, true", "-9, false"})
    public void test1(int param1, boolean param2) {
        values.add("t");
        if (param1 == 42 && param2) {
            throw new IllegalStateException();
        }
    }

    @BeforeClass
    public static void beforeClass() {
        values.add("bc");
    }

    @AfterClass
    public static void afterClass() {
        values.add("ac");
    }
}