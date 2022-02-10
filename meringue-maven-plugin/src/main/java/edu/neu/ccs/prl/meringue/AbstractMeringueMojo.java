package edu.neu.ccs.prl.meringue;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
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
    private File javaExec = PluginUtil.javaHomeToJavaExec(new File(System.getProperty("java.home")));
    /**
     * The non-empty, non-null, fully-qualified name of the fuzzing framework that should be used.
     */
    @Parameter(property = "meringue.framework", readonly = true, required = true)
    private String framework;
    /**
     * Configuration options that should be passed to the fuzzing framework.
     */
    @Parameter(readonly = true)
    private Properties options = new Properties();

    File getOutputDir() {
        return outputDir;
    }

    String getTestClass() {
        return testClass;
    }

    String getTestMethod() {
        return testMethod;
    }

    Properties getOptions() {
        return options;
    }

    File getJavaExec() throws MojoExecutionException {
        if (PluginUtil.isInvalidJavaExecutable(javaExec)) {
            throw new MojoExecutionException("Invalid Java executable: " + javaExec);
        }
        return javaExec;
    }

    CampaignConfiguration createConfiguration(Class<?> mainClass, File... additionalClasspathElements)
            throws MojoExecutionException {
        try {
            PluginUtil.ensureDirectory(getOutputDir());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create output directory", e);
        }
        File manifestJar = createManifestJar(mainClass, additionalClasspathElements);
        return new CampaignConfiguration(getOutputDir(), getTestClass(), getTestMethod(), getJavaExec(), manifestJar);
    }

    Set<File> getTestClasspathFiles() throws MojoExecutionException {
        try {
            return project.getTestClasspathElements().stream().map(File::new).collect(Collectors.toSet());
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Required test dependency was not resolved", e);
        }
    }

    File createManifestJar(Class<?> mainClass, File... additionalClasspathElements)
            throws MojoExecutionException {
        try {
            Set<File> classpathFiles = new HashSet<>(getTestClasspathFiles());
            classpathFiles.addAll(Arrays.asList(additionalClasspathElements));
            classpathFiles.add(PluginUtil.getClassPathElement(mainClass));
            File jar = PluginUtil.buildManifestJar(classpathFiles, mainClass, outputDir);
            jar.deleteOnExit();
            return jar;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create manifest jar", e);
        }
    }

    String getTestDescription() {
        return testClass + "#" + testMethod;
    }

    FuzzFramework createFramework() throws MojoExecutionException {
        try {
            Class<?> clazz = Class.forName(framework);
            return (FuzzFramework) clazz.newInstance();
        } catch (ClassCastException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new MojoExecutionException("Failed to create fuzzing framework instance", e);
        }
    }
}