package edu.neu.ccs.prl.meringue.param;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class ConstructorParamTest {
    public final int param1;
    public final long param2;

    public ConstructorParamTest(int param1, long param2) {
        this.param1 = param1;
        this.param2 = param2;
    }

    @Test
    public void test() {
        if (param2 == param1) {
            throw new IllegalStateException();
        }
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