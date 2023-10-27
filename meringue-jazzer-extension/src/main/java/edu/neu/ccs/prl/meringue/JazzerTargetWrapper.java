package edu.neu.ccs.prl.meringue;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

import java.lang.reflect.InvocationTargetException;

public final class JazzerTargetWrapper {
    private static JazzerTarget target;

    public static void fuzzerInitialize(String[] args) {
        target = new JazzerTarget(args[0], args[1], JazzerTargetWrapper.class.getClassLoader());
    }

    public static void fuzzerTestOneInput(FuzzedDataProvider provider) throws Throwable {
        try {
            target.execute(provider);
        } catch (InvocationTargetException t) {
            throw t.getTargetException();
        }
    }
}
