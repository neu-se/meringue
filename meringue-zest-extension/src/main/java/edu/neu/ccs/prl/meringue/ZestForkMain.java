package edu.neu.ccs.prl.meringue;

import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;

import java.io.File;

public final class ZestForkMain {
    private ZestForkMain() {
        throw new AssertionError(getClass().getSimpleName() + " is a static utility class and should " +
                                         "not be instantiated");
    }

    public static void main(String[] args) throws Throwable {
        try {
            String testClassName = args[0];
            String testMethodName = args[1];
            File outputDir = new File(args[2]);
            Guidance guidance = new ZestGuidance(testClassName + "#" + testMethodName, null, outputDir);
            GuidedFuzzing.run(testClassName, testMethodName, guidance, System.out);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }
}