package edu.neu.ccs.prl.meringue;

import edu.neu.ccs.prl.meringue.internal.CampaignConfiguration;
import edu.neu.ccs.prl.meringue.internal.FuzzFramework;
import edu.neu.ccs.prl.meringue.internal.FileUtil;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.booter.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

abstract class AbstractMeringueMojo extends AbstractMojo {
    /**
     * The current Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
    /**
     * Directory to which output files should be written.
     */
    @Parameter(property = "meringue.outputDir", defaultValue = "${project.build.directory}/meringue")
    private File outputDir;
    /**
     * The non-empty, non-null, fully-qualified name of the test class.
     *
     * @see Class#forName(String className)
     */
    @Parameter(property = "meringue.testClass", required = true)
    private String testClass;
    /**
     * The non-empty, non-null name of the test method.
     */
    @Parameter(property = "meringue.testMethod", required = true)
    private String testMethod;
    /**
     * The Java executable that should be used.
     * If not specified, the executable used to run Maven will be used.
     */
    @Parameter(property = "meringue.javaExec")
    private File javaExec = FileUtil.javaHomeToJavaExec(new File(System.getProperty("java.home")));
    /**
     * The non-empty, non-null, fully-qualified name of the fuzzing framework that should be used.
     */
    @Parameter(property = "meringue.framework", readonly = true, required = true)
    private String framework;
    /**
     * Configuration options that should be passed to the fuzzing framework.
     */
    @Parameter(readonly = true)
    private Properties frameworkOptions = new Properties();
    /**
     * Java command line options that should be used for the forked JVM.
     */
    @Parameter(property = "meringue.javaOptions")
    private List<String> javaOptions = new ArrayList<>();
    /**
     * Textual representation of the maximum amount of time to execute the fuzzing campaign in the ISO-8601 duration
     * format. The default value is one day.
     * <p>
     * {@link java.time.Duration#parse(CharSequence)}
     */
    @Parameter(property = "meringue.duration", defaultValue = "P1D")
    private String duration;

    String getTestDescription() {
        return testClass + "#" + testMethod;
    }

    CampaignConfiguration createConfiguration() throws MojoExecutionException {
        validateJavaExec();
        initializeOutputDir();
        return new CampaignConfiguration(testClass, testMethod, Duration.parse(duration), outputDir,
                javaOptions, javaExec, createClassPathJar());
    }

    Set<File> getTestClasspathElements() throws MojoExecutionException {
        try {
            return project.getTestClasspathElements().stream().map(File::new).collect(Collectors.toSet());
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Required test dependency was not resolved", e);
        }
    }

    FuzzFramework createFramework(CampaignConfiguration config) throws MojoExecutionException {
        try {
            FuzzFramework instance = (FuzzFramework) Class.forName(framework).getDeclaredConstructor().newInstance();
            instance.initialize(config, frameworkOptions);
            return instance;
        } catch (ClassCastException | ReflectiveOperationException e) {
            throw new MojoExecutionException("Failed to create fuzzing framework instance", e);
        }
    }

    private void validateJavaExec() throws MojoExecutionException {
        if (!SystemUtils.endsWithJavaPath(javaExec.getAbsolutePath()) || !javaExec.isFile()) {
            throw new MojoExecutionException("Invalid Java executable: " + javaExec);
        }
    }

    private void initializeOutputDir() throws MojoExecutionException {
        try {
            FileUtil.ensureDirectory(outputDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create output directory", e);
        }
    }

    private File createClassPathJar() throws MojoExecutionException {
        try {
            File jar = new File(outputDir, "test-class-path.jar");
            FileUtil.buildManifestJar(getTestClasspathElements(), jar);
            return jar;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create test class path JAR", e);
        }
    }
}