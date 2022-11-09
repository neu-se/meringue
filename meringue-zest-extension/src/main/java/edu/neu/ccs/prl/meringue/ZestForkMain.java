package edu.neu.ccs.prl.meringue;

import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;

import java.io.File;

public final class ZestForkMain {
    public static final String SYSTEM_PROPERTIES_KEY = "zest.properties";

    private ZestForkMain() {
        throw new AssertionError(
                getClass().getSimpleName() + " is a static utility class and should not be instantiated");
    }

    public static void main(String[] args) throws Throwable {
        try {
            String testClassName = args[0];
            String testMethodName = args[1];
            File outputDirectory = new File(args[2]);
            // Note: must set system properties before loading the test class
            SystemPropertyUtil.loadSystemProperties(SYSTEM_PROPERTIES_KEY);
            Guidance guidance = new ZestGuidance(testClassName + "#" + testMethodName, null,
                                                 outputDirectory);
            GuidedFuzzing.run(testClassName, testMethodName, ZestForkMain.class.getClassLoader(), guidance, System.out);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }
}