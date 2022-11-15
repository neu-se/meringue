package edu.neu.ccs.prl.meringue;

import java.io.File;
import java.io.Serializable;
import java.time.Duration;
import java.util.*;

/**
 * Configuration for a fuzzing campaign. Immutable.
 */
public final class CampaignConfiguration implements Serializable {
    private static final long serialVersionUID = -1880940974469925517L;
    /**
     * Directory to which output files should be written.
     * <p>
     * Non-null.
     */
    private final File outputDirectory;
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
    private final File javaExecutable;
    /**
     * JAR file containing all necessary classpath elements for test JVMs.
     * <p>
     * Non-null.
     */
    private final File testClasspathJar;
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
     * Unmodifiable map of specifying environment variable settings for test JVMs or {@code null} if test JVMs should
     * inherit the environment of the current process.
     *
     * @see ProcessBuilder#environment()
     */
    private final Map<String, String> environment;
    /**
     * Working directory for test JVMs or {@code null} if test JVMs should inherit the working directory of the current
     * process.
     *
     * @see ProcessBuilder#directory()
     */
    private final File workingDirectory;

    public CampaignConfiguration(String testClassName, String testMethodName, Duration duration, File outputDirectory,
                                 List<String> javaOptions, File testClasspathJar, File javaExecutable,
                                 File workingDirectory, Map<String, String> environment) {
        if (testClassName.isEmpty() || testMethodName.isEmpty() || duration.isNegative() ||
                !outputDirectory.isDirectory() || !testClasspathJar.isFile() || !javaExecutable.isFile() ||
                (workingDirectory != null && !workingDirectory.isDirectory())) {
            throw new IllegalArgumentException();
        }
        this.testClassName = testClassName;
        this.testMethodName = testMethodName;
        this.duration = duration;
        this.outputDirectory = outputDirectory;
        this.javaOptions = Collections.unmodifiableList(new ArrayList<>(javaOptions));
        for (String option : javaOptions) {
            if (option == null) {
                throw new NullPointerException();
            }
        }
        this.testClasspathJar = testClasspathJar;
        this.javaExecutable = javaExecutable;
        this.environment = environment == null ? null : Collections.unmodifiableMap(new HashMap<>(environment));
        this.workingDirectory = workingDirectory;
    }

    public Duration getDuration() {
        return duration;
    }

    public List<String> getJavaOptions() {
        return javaOptions;
    }

    public File getTestClasspathJar() {
        return testClasspathJar;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public String getTestClassName() {
        return testClassName;
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    public String getTestDescription() {
        return testClassName + "#" + testMethodName;
    }

    public File getJavaExecutable() {
        return javaExecutable;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }
}
