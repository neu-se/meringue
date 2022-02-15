package edu.neu.ccs.prl.meringue;

import java.io.File;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CampaignConfiguration implements Serializable {
    private static final long serialVersionUID = -1880940974469925517L;
    /**
     * Directory to which output files should be written.
     * <p>
     * Non-null.
     */
    private final File outputDir;
    /**
     * Fully-qualified name of the test class.
     * <p>
     * Non-null, non-empty.
     *
     * @see Class#forName(String className)
     */
    private final String testClassName;
    /**
     * Name of the test method.
     * <p>
     * Non-null, non-empty.
     */
    private final String testMethodName;
    /**
     * Java executable that should be used for test JVMs.
     * <p>
     * Non-null.
     */
    private final File javaExec;
    /**
     * JAR file containing all necessary class path elements for test JVMs.
     * <p>
     * Non-null.
     */
    private final File testClassPathJar;
    /**
     * Maximum amount of time to execute the campaign for.
     * <p>
     * Non-negative, non-null.
     */
    private final Duration duration;
    /**
     * Java command line options that should be used for test JVMs.
     * <p>
     * Non-null, contains no null elements, unmodifiable.
     */
    private final List<String> javaOptions;
    /**
     * True if test JVMs should suspend and wait for a debugger to attach.
     */
    private final boolean debug;

    public CampaignConfiguration(String testClassName, String testMethodName, Duration duration, File outputDir,
                                 List<String> javaOptions, File testClassPathJar, File javaExec, boolean debug) {
        if (testClassName.isEmpty() || testMethodName.isEmpty() || duration.isNegative()
                || !outputDir.isDirectory() || !testClassPathJar.isFile() || !javaExec.isFile()) {
            throw new IllegalArgumentException();
        }
        this.testClassName = testClassName;
        this.testMethodName = testMethodName;
        this.duration = duration;
        this.outputDir = outputDir;
        this.javaOptions = Collections.unmodifiableList(new ArrayList<>(javaOptions));
        for (String option : javaOptions) {
            if (option == null) {
                throw new NullPointerException();
            }
        }
        this.testClassPathJar = testClassPathJar;
        this.javaExec = javaExec;
        this.debug = debug;
    }

    public Duration getDuration() {
        return duration;
    }

    public List<String> getJavaOptions() {
        return javaOptions;
    }

    public File getTestClassPathJar() {
        return testClassPathJar;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public String getTestClassName() {
        return testClassName;
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    public File getJavaExec() {
        return javaExec;
    }

    public boolean isDebug() {
        return debug;
    }
}
