package edu.berkeley.cs.jqf.examples;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
@SuppressWarnings("all")
public class ParameterizedConstructorExample {
    public final int param1;
    public final long param2;

    public ParameterizedConstructorExample(int param1, long param2) {
        this.param1 = param1;
        this.param2 = param2;
    }

    @Test
    public void test1() {
        System.out.println((param1 == 42) ? param2 : "Hello world");
    }

    @Test
    public void test2() {
    }

    @Parameterized.Parameters
    public static Collection<?> arguments() {
        return Arrays.asList(new Object[][]{
                {2, 55L},
                {6, -9L},
                {19, 0L},
        });
    }
}