package edu.neu.ccs.prl.meringue;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

import java.lang.reflect.InvocationTargetException;

public final class JazzerTarget {
    private static Throwable lastThrown = null;
    private static FuzzTarget target;
    private static boolean rethrow = true;

    private JazzerTarget() {
        throw new AssertionError(getClass().getSimpleName() + " is a static utility class and should " +
                "not be instantiated");
    }

    public static void fuzzerInitialize(String[] args) {
        try {
            // TODO validate target's parameters (should be byte[] or FuzzedDataProvider
            target = new FuzzTarget(args[0], args[1], JazzerTarget.class.getClassLoader());
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    public static void fuzzerTestOneInput(FuzzedDataProvider provider) throws Throwable {
        lastThrown = null;
        try {
            // TODO handle methods that take byte[] or FuzzedDataProvider argument
            target.execute(new Object[]{provider.consumeRemainingAsBytes()});
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
