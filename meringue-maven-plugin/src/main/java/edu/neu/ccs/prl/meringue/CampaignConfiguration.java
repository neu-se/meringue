package edu.neu.ccs.prl.meringue;

import java.io.File;

public final class CampaignConfiguration {
    /**
     * Directory to which output files should be written.
     */
    private final File outputDir;
    /**
     * The non-empty, non-null, fully-qualified name of the test class.
     *
     * @see Class#forName(String className)
     */
    private final String testClassName;
    /**
     * The non-empty, non-null name of the test method.
     */
    private final String testMethodName;
    /**
     * The Java executable that should be used to run the campaign.
     */
    private final File javaExec;
    /**
     * JAR file specifying the test class path.
     */
    private final File testClassPathJar;

    public CampaignConfiguration(File outputDir, String testClassName, String testMethodName, File javaExec,
                                 File testClassPathJar) {
        if (outputDir == null || testMethodName == null || testClassName == null) {
            throw new NullPointerException();
        }
        if (PluginUtil.isInvalidJavaExecutable(javaExec) || !testClassPathJar.isFile()) {
            throw new IllegalArgumentException();
        }
        this.outputDir = outputDir;
        this.testClassName = testClassName;
        this.testMethodName = testMethodName;
        this.javaExec = javaExec;
        this.testClassPathJar = testClassPathJar;
        // TODO add support for JVM options
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

    public File getTestClassPathJar() {
        return testClassPathJar;
    }
}
