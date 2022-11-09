package edu.neu.ccs.prl.meringue;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
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
    public MavenSession getSession() {
        return session;
    }

    @Override
    public MavenProject getProject() {
        return project;
    }

    @Override
    public File getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    public String getTestClassName() {
        return testClass;
    }

    @Override
    public String getTestMethodName() {
        return testMethod;
    }

    @Override
    public File getJavaExecutable() {
        return javaExec;
    }

    @Override
    public String getFrameworkClassName() {
        return framework;
    }

    @Override
    public Properties getFrameworkArguments() {
        return frameworkArguments;
    }

    @Override
    public List<String> getJavaOptions() {
        return javaOptions;
    }

    @Override
    public String getDurationString() {
        return duration;
    }

    @Override
    public File getTemporaryDirectory() {
        return temporaryDirectory;
    }

    @Override
    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }

    @Override
    public Map<String, Artifact> getPluginArtifactMap() {
        return pluginArtifactMap;
    }

    @Override
    public ResolutionErrorHandler getErrorHandler() {
        return errorHandler;
    }

    @Override
    public RepositorySystem getRepositorySystem() {
        return repositorySystem;
    }

    @Override
    public Map<String, String> getEnvironment() {
        return null;
    }

    @Override
    public File getWorkingDirectory() {
        return null;
    }
}