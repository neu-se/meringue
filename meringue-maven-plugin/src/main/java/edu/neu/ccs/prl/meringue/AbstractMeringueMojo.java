package edu.neu.ccs.prl.meringue;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

abstract class AbstractMeringueMojo extends AbstractMojo implements CampaignValues {
    /**
     * Directory to which output files should be written.
     */
    @Parameter(property = "meringue.outputDirectory", defaultValue = "${project.build.directory}/meringue")
    private File outputDirectory;
    /**
     * Fully-qualified name of the test class.
     *
     * @see Class#forName(String className)
     */
    @Parameter(property = "meringue.testClass", required = true)
    private String testClass;
    /**
     * Name of the test method.
     */
    @Parameter(property = "meringue.testMethod", required = true)
    private String testMethod;
    /**
     * Java executable that should be used. If not specified, the executable used to run Maven will be used.
     */
    @Parameter(property = "meringue.javaExec")
    private File javaExec = FileUtil.javaHomeToJavaExec(new File(System.getProperty("java.home")));
    /**
     * Fully-qualified name of the fuzzing framework that should be used.
     */
    @Parameter(property = "meringue.framework", readonly = true, required = true)
    private String framework;
    /**
     * Arguments used to configure the fuzzing framework.
     */
    @Parameter(readonly = true)
    private Properties frameworkArguments = new Properties();
    /**
     * Java command line options that should be used for test JVMs.
     */
    @Parameter(property = "meringue.javaOptions")
    private List<String> javaOptions = new ArrayList<>();
    /**
     * Textual representation of the maximum amount of time to execute the fuzzing campaign in the ISO-8601 duration
     * format. The default value is one day.
     * <p>
     * See {@link java.time.Duration#parse(CharSequence)}.
     */
    @Parameter(property = "meringue.duration", defaultValue = "P1D")
    private String duration;
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
    @Parameter(defaultValue = "${project.build.directory}/temp/meringue", readonly = true, required = true)
    private File temporaryDirectory;
    @Parameter(property = "plugin.artifactMap", required = true, readonly = true)
    private Map<String, Artifact> pluginArtifactMap;
    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
    private ArtifactRepository localRepository;
    @Component
    private ResolutionErrorHandler errorHandler;
    @Component
    private RepositorySystem repositorySystem;

    @Override
    public MavenSession getSession() throws MojoExecutionException {
        return session;
    }

    @Override
    public MavenProject getProject() throws MojoExecutionException {
        return project;
    }

    @Override
    public File getOutputDirectory() throws MojoExecutionException {
        return outputDirectory;
    }

    @Override
    public String getTestClassName() throws MojoExecutionException {
        return testClass;
    }

    @Override
    public String getTestMethodName() throws MojoExecutionException {
        return testMethod;
    }

    @Override
    public File getJavaExecutable() throws MojoExecutionException {
        return javaExec;
    }

    @Override
    public String getFrameworkClassName() throws MojoExecutionException {
        return framework;
    }

    @Override
    public Properties getFrameworkArguments() throws MojoExecutionException {
        return frameworkArguments;
    }

    @Override
    public List<String> getJavaOptions() throws MojoExecutionException {
        return javaOptions;
    }

    @Override
    public String getDurationString() throws MojoExecutionException {
        return duration;
    }

    @Override
    public File getTemporaryDirectory() throws MojoExecutionException {
        return temporaryDirectory;
    }

    @Override
    public ArtifactRepository getLocalRepository() throws MojoExecutionException {
        return localRepository;
    }

    @Override
    public Map<String, Artifact> getPluginArtifactMap() throws MojoExecutionException {
        return pluginArtifactMap;
    }

    @Override
    public ResolutionErrorHandler getErrorHandler() throws MojoExecutionException {
        return errorHandler;
    }

    @Override
    public RepositorySystem getRepositorySystem() throws MojoExecutionException {
        return repositorySystem;
    }

    @Override
    public Map<String, String> getEnvironment() throws MojoExecutionException {
        return null;
    }

    @Override
    public File getWorkingDirectory() throws MojoExecutionException {
        return null;
    }
}