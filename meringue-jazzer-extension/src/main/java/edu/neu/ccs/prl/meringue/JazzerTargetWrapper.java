package edu.neu.ccs.prl.meringue;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

import java.lang.reflect.InvocationTargetException;

public final class JazzerTargetWrapper {
    private static Throwable lastThrown = null;
    private static JazzerTarget target;
    private static boolean rethrow = true;

    private JazzerTargetWrapper() {
        throw new AssertionError(getClass().getSimpleName() + " is a static utility class and should " +
                "not be instantiated");
    }

    public static void fuzzerInitialize(String[] args) {
        try {
            target = new JazzerTarget(args[0], args[1], JazzerTargetWrapper.class.getClassLoader());
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    public static void fuzzerTestOneInput(FuzzedDataProvider provider) throws Throwable {
        lastThrown = null;
        try {
            target.execute(provider);
        } catch (InvocationTargetException t) {
            lastThrown = t.getTargetException();
            if (rethrow) {
                throw lastThrown;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    public static void setRethrow(boolean value) {
        rethrow = value;
    }

    public static Throwable getLastThrown() {
        return lastThrown;
    }
}
