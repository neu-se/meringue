package edu.berkeley.cs.jqf;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@RunWith(Parameterized.class)
@SuppressWarnings("all")
public class FieldParamExample {
    public static final List<String> values = new LinkedList<>();
    @Parameterized.Parameter(value = 1)
    public int param1;

    @Parameterized.Parameter()
    public String param2;

    @Before
    public void before() {
        values.add("b");
    }

    @After
    public void after() {
        values.add("a");
    }

    @Test
    public void test1() {
        values.add("t");
        if (param1 == 42) {
            throw new IllegalStateException();
        }
    }

    @BeforeClass
    public static void beforeClass() {
        values.add("bc");
    }

    @Parameterized.BeforeParam
    public static void beforeParam() {
        values.add("bp");
    }

    @AfterClass
    public static void afterClass() {
        values.add("ac");
    }

    @Parameterized.AfterParam
    public static void afterParam() {
        values.add("ap");
    }

    @Parameterized.Parameters
    public static Collection<?> arguments() {
        return Arrays.asList(new Object[][]{{"hello", 2}, {"", -2},});
    }
}