package edu.neu.ccs.prl.meringue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class FieldParamTest {
    @Parameterized.Parameter(value = 1)
    public int param1;

    @Parameterized.Parameter()
    public String param2;

    @Test
    public void test() {
        int i = param2.length();
        if (i == param1) {
            throw new IllegalStateException();
        }
    }

    @Parameterized.Parameters
    public static Collection<?> arguments() {
        return Arrays.asList(new Object[][]{{"hello", 2}, {"", -2},});
    }
}
