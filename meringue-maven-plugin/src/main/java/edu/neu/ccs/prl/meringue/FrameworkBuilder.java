package edu.neu.ccs.prl.meringue;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class FrameworkBuilder {
    private String frameworkClassName;
    private Properties frameworkArguments;
    private DependencyResolver resolver;
    private Map<String, Artifact> pluginArtifactMap;
    private File temporaryDirectory;

    public FrameworkBuilder frameworkClassName(String frameworkClassName) {
        this.frameworkClassName = frameworkClassName;
        return this;
    }

    public FrameworkBuilder frameworkArguments(Properties frameworkArguments) {
        this.frameworkArguments = frameworkArguments;
        return this;
    }

    public FrameworkBuilder resolver(DependencyResolver resolver) {
        this.resolver = resolver;
        return this;
    }

    public FrameworkBuilder pluginArtifactMap(Map<String, Artifact> pluginArtifactMap) {
        this.pluginArtifactMap = pluginArtifactMap;
        return this;
    }

    public FrameworkBuilder temporaryDirectory(File temporaryDirectory) {
        this.temporaryDirectory = temporaryDirectory;
        return this;
    }

    public FuzzFramework build(CampaignConfiguration configuration) throws MojoExecutionException {
        validate();
        try {
            FuzzFramework framework = (FuzzFramework) Class.forName(frameworkClassName)
                    .getDeclaredConstructor()
                    .newInstance();
            if (framework instanceof JarFuzzFramework) {
                JarFuzzFramework jFramework = (JarFuzzFramework) framework;
                jFramework.setFrameworkJar(buildFrameworkJar(jFramework.getCoordinate()));
            }
            framework.setResolver(resolver);
            framework.setTemporaryDirectory(temporaryDirectory);
            framework.initialize(configuration, frameworkArguments);
            return framework;
        } catch (ClassCastException | ReflectiveOperationException | IOException e) {
            throw new MojoExecutionException("Failed to create fuzzing framework instance", e);
        }
    }

    private void validate() {
        if (frameworkClassName == null) {
            throw new IllegalStateException("Missing frameworkClassName");
        } else if (frameworkArguments == null) {
            throw new IllegalStateException("Missing frameworkArguments");
        } else if (resolver == null) {
            throw new IllegalStateException("Missing resolver");
        } else if (pluginArtifactMap == null) {
            throw new IllegalStateException("Missing pluginArtifactMap");
        } else if (temporaryDirectory == null) {
            throw new IllegalStateException("Missing temporaryDirectory");
        }
    }

    private File buildFrameworkJar(String coordinate) throws MojoExecutionException {
        try {
            if (!pluginArtifactMap.containsKey(coordinate)) {
                throw new MojoExecutionException("Unknown plugin artifact: " + coordinate);
            }
            Artifact artifact = pluginArtifactMap.get(coordinate);
            File jar = new File(temporaryDirectory, "framework.jar");
            FileUtil.buildManifestJar(resolver.resolve(artifact), jar);
            return jar;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create framework arguments", e);
        }
    }
}
