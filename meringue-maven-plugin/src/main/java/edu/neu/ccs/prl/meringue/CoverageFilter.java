package edu.neu.ccs.prl.meringue;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.jacoco.agent.rt.internal_aeaf9ab.PreMain;
import org.jacoco.core.runtime.WildcardMatcher;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class CoverageFilter {
    private final ClassFilter classFilter;
    private final MavenProject project;
    private final WildcardMatcher includes;
    private final WildcardMatcher excludes;
    private final ArtifactSourceResolver resolver;
    private final File javaExecutable;

    public CoverageFilter(AnalysisValues values) throws MojoExecutionException {
        this(new ClassFilter(values), values.getProject(), values.createArtifactSourceResolver(),
             values.includeJavaClassLibrary() ? values.getJavaExecutable() : null);
    }

    @Deprecated
    public CoverageFilter(Collection<String> inclusions, Collection<String> exclusions,
                          List<String> includedArtifacts, MavenProject project, ArtifactSourceResolver resolver) {
        this(new ClassFilter(inclusions, exclusions, includedArtifacts), project, resolver, null);
    }

    public CoverageFilter(ClassFilter classFilter, MavenProject project, ArtifactSourceResolver resolver,
                          File javaExecutable) {
        if (classFilter == null || project == null || resolver == null) {
            throw new NullPointerException();
        }
        this.classFilter = classFilter;
        this.project = project;
        this.resolver = resolver;
        this.javaExecutable = javaExecutable;
        String includeString = classFilter.getIncludeString();
        this.includes = new WildcardMatcher(toVMName(includeString.isEmpty() ? "*" : includeString));
        this.excludes = new WildcardMatcher(toVMName(classFilter.getExcludeString()));
    }

    public boolean includeJavaClassLibrary() {
        return javaExecutable != null;
    }

    public File getJavaHome() {
        return FileUtil.javaExecToJavaHome(javaExecutable);
    }

    public ClassFilter getClassFilter() {
        return classFilter;
    }

    public Set<File> getIncludedArtifacts() {
        Set<File> result = new HashSet<>();
        if (classFilter.isIncludedArtifact(project.getGroupId(), project.getArtifactId())
                && project.getBuild().getOutputDirectory() != null) {
            result.add(new File(project.getBuild().getOutputDirectory()));
        }
        project.getArtifacts()
               .stream()
               .filter(a -> classFilter.isIncludedArtifact(a) && a.getArtifactHandler().isAddedToClasspath())
               .map(Artifact::getFile)
               .filter(Objects::nonNull)
               .forEach(result::add);
        return result;
    }

    public Set<File> getIncludedArtifactSources() {
        Set<File> sources = new HashSet<>();
        sources.add(new File(project.getBuild().getTestSourceDirectory()));
        sources.add(new File(project.getBuild().getSourceDirectory()));
        sources.addAll(resolver.getSources(project, project.getArtifacts()
                                                           .stream()
                                                           .filter(a -> classFilter.isIncludedArtifact(a) &&
                                                                   a.getArtifactHandler().isAddedToClasspath())
                                                           .collect(Collectors.toSet())));
        sources.removeIf(f -> f == null || !f.exists());
        return sources;
    }

    boolean filter(String className) {
        return includes.matches(className) && !excludes.matches(className);
    }

    public CoverageCalculator createCoverageCalculator(File temporaryDirectory) throws IOException {
        try {
            return new CoverageCalculator(temporaryDirectory, this);
        } catch (IOException e) {
            throw new IOException("Failed to initialize JaCoCo coverage calculator", e);
        }
    }

    public String getJacocoOption() {
        File agentJar = FileUtil.getClassPathElement(PreMain.class);
        if (!agentJar.exists()) {
            throw new IllegalStateException("JaCoCo agent jar does not exist: " + agentJar);
        }
        String opt = String.format("-javaagent:%s=output=none", agentJar.getAbsolutePath());
        String excludeString = classFilter.getExcludeString();
        if (!excludeString.isEmpty()) {
            opt += ",excludes=" + excludeString;
        }
        String includeString = classFilter.getIncludeString();
        if (!includeString.isEmpty()) {
            opt += ",includes=" + includeString;
        }
        if (javaExecutable != null) {
            opt += ",inclbootstrapclasses=true";
        }
        return opt;
    }

    private static String toVMName(final String srcName) {
        return srcName.replace('.', '/');
    }
}
