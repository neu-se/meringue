package edu.berkeley.cs.jqf.examples;

import com.pholser.junit.quickcheck.From;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
@SuppressWarnings("all")
public class ParameterizedFieldFromExample {
    @Parameterized.Parameter()
    @From(MyStringGenerator.class)
    public String param1;

    @Test
    public void test1() {
        if (param1.equals("hello")) {
            throw new IllegalArgumentException();
        }
    }

    @Parameterized.Parameters
    public static Collection<?> arguments() {
        return Arrays.asList(new Object[][]{{"hello", 2}, {"", -2},});
    }
}