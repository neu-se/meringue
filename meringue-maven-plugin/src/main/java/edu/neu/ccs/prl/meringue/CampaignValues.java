package edu.neu.ccs.prl.meringue;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.surefire.booter.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public interface CampaignValues {
    /**
     * Current Maven session.
     */
    MavenSession getSession() throws MojoExecutionException;

    /**
     * Current Maven project.
     */
    MavenProject getProject() throws MojoExecutionException;

    /**
     * Directory to which output files should be written.
     */
    File getOutputDirectory() throws MojoExecutionException;

    /**
     * Fully-qualified name of the test class.
     *
     * @see Class#forName(String className)
     */
    String getTestClassName() throws MojoExecutionException;

    /**
     * Name of the test method.
     */
    String getTestMethodName() throws MojoExecutionException;

    /**
     * Java executable that should be used.
     */
    File getJavaExecutable() throws MojoExecutionException;

    /**
     * Fully-qualified name of the fuzzing framework that should be used.
     */
    String getFrameworkClassName() throws MojoExecutionException;

    /**
     * Arguments used to configure the fuzzing framework.
     */
    Properties getFrameworkArguments() throws MojoExecutionException;

    /**
     * Java command line options that should be used for campaign JVMs.
     */
    List<String> getJavaOptions() throws MojoExecutionException;

    /**
     * Textual representation of the maximum amount of time to execute the fuzzing campaign in the ISO-8601 duration
     * format.
     * <p>
     * See {@link java.time.Duration#parse(CharSequence)}.
     */
    String getDurationString() throws MojoExecutionException;

    /**
     * Directory used to store internal temporary files.
     */
    File getTemporaryDirectory() throws MojoExecutionException;

    /**
     * The local Maven artifact repository.
     */
    ArtifactRepository getLocalRepository() throws MojoExecutionException;

    /**
     * Mapping from Maven coordinates (of the form groupId:artifactId) to artifacts. Contains coordinates for artifact
     * on which this plugin is dependent.
     */
    Map<String, Artifact> getPluginArtifactMap() throws MojoExecutionException;

    /**
     * Resolution error handler for this Maven session.
     */

    ResolutionErrorHandler getErrorHandler() throws MojoExecutionException;

    /**
     * Repository system for this Maven session.
     */
    RepositorySystem getRepositorySystem() throws MojoExecutionException;

    /**
     * Logger for this Maven plugin.
     *
     * @see org.apache.maven.plugin.Mojo#getLog()
     */
    Log getLog() throws MojoExecutionException;

    /**
     * Map of specifying environment variable settings for campaign JVMs or {@code null} if campaign JVMs should inherit
     * the environment of the current process.
     *
     * @see ProcessBuilder#environment()
     */
    Map<String, String> getEnvironment() throws MojoExecutionException;

    /**
     * Working directory for campaign JVMs or {@code null} if campaign JVMs should inherit the working directory of the
     * current process.
     *
     * @see ProcessBuilder#directory()
     */
    File getWorkingDirectory() throws MojoExecutionException;

    /**
     * Artifact handler manager for this Maven session.
     */
    ArtifactHandlerManager getArtifactHandlerManager() throws MojoExecutionException;

    default DependencyResolver createDependencyResolver() throws MojoExecutionException {
        return new DependencyResolver(this);
    }

    default FrameworkBuilder createFrameworkBuilder() throws MojoExecutionException {
        return new FrameworkBuilder().frameworkClassName(getFrameworkClassName())
                                     .frameworkArguments(getFrameworkArguments())
                                     .pluginArtifactMap(getPluginArtifactMap()).resolver(createDependencyResolver())
                                     .temporaryDirectory(getTemporaryDirectory());
    }

    default Duration getDuration() throws MojoExecutionException {
        return Duration.parse(getDurationString());
    }

    default CampaignConfiguration createCampaignConfiguration() throws MojoExecutionException {
        return new CampaignConfiguration(getTestClassName(), getTestMethodName(), getDuration(), getCampaignDirectory(),
                                         getJavaOptions(), createTestJar(), getJavaExecutable(), getWorkingDirectory(),
                                         getEnvironment());
    }

    default Set<File> getTestClasspathElements() throws MojoExecutionException {
        try {
            return getProject().getTestClasspathElements().stream().map(File::new).collect(Collectors.toSet());
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Required test dependency was not resolved", e);
        }
    }

    default void initialize() throws MojoExecutionException {
        if (!SystemUtils.endsWithJavaPath(getJavaExecutable().getAbsolutePath()) || !getJavaExecutable().isFile()) {
            throw new MojoExecutionException("Invalid Java executable: " + getJavaExecutable());
        }
        try {
            FileUtil.ensureDirectory(getOutputDirectory());
            FileUtil.ensureDirectory(getTemporaryDirectory());
            FileUtil.ensureDirectory(getCampaignDirectory());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to initialize directory", e);
        }
    }

    default File getCampaignDirectory() throws MojoExecutionException {
        return new File(getOutputDirectory(), "campaign");
    }

    default File createTestJar() throws MojoExecutionException {
        try {
            File jar = new File(getTemporaryDirectory(), "test.jar");
            FileUtil.buildManifestJar(getTestClasspathElements(), jar);
            return jar;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create test manifest JAR", e);
        }
    }

    default String getTestDescription() throws MojoExecutionException {
        return getTestClassName() + "#" + getTestMethodName();
    }

    default void fuzz() throws MojoExecutionException {
        new CampaignRunner(this).run();
    }
}
