package edu.neu.ccs.prl.meringue;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.jacoco.agent.rt.internal_3570298.PreMain;
import org.jacoco.core.runtime.WildcardMatcher;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class CoverageFilter {
    private final List<String> inclusions;
    private final List<String> exclusions;
    private final Set<String> includedArtifacts;
    private final MavenProject project;
    private final WildcardMatcher includes;
    private final WildcardMatcher excludes;
    private final ArtifactSourceResolver resolver;

    public CoverageFilter(Collection<String> inclusions, Collection<String> exclusions,
                          List<String> includedArtifacts, MavenProject project, ArtifactSourceResolver resolver) {
        this.inclusions = inclusions == null ? Collections.emptyList() : new ArrayList<>(inclusions);
        this.exclusions = exclusions == null ? Collections.emptyList() : new ArrayList<>(exclusions);
        this.includedArtifacts =
                includedArtifacts == null ? Collections.emptySet() : new HashSet<>(includedArtifacts);
        this.project = project;
        this.resolver = resolver;
        this.includes =
                new WildcardMatcher(toVMName(this.inclusions.isEmpty() ? "*" : String.join(":", this.inclusions)));
        this.excludes = new WildcardMatcher(toVMName(String.join(":", this.exclusions)));
    }

    public Set<File> getIncludedArtifacts() {
        Set<File> result = new HashSet<>();
        if (shouldIncludeArtifact(project.getGroupId(), project.getArtifactId())
                && project.getBuild().getOutputDirectory() != null) {
            result.add(new File(project.getBuild().getOutputDirectory()));
        }
        project.getArtifacts()
               .stream()
               .filter(a -> shouldIncludeArtifact(a) && a.getArtifactHandler().isAddedToClasspath())
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
                                                           .filter(a -> shouldIncludeArtifact(a) &&
                                                                   a.getArtifactHandler().isAddedToClasspath())
                                                           .collect(Collectors.toSet())));
        sources.removeIf(f -> f == null || !f.exists());
        return sources;
    }

    boolean filter(String className) {
        return includes.matches(className) && !excludes.matches(className);
    }

    private boolean shouldIncludeArtifact(Artifact artifact) {
        return shouldIncludeArtifact(artifact.getGroupId(), artifact.getArtifactId());
    }

    private boolean shouldIncludeArtifact(String groupId, String artifactId) {
        return includedArtifacts.isEmpty() || includedArtifacts.contains(groupId + ":" + artifactId);
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
        if (!exclusions.isEmpty()) {
            opt += ",excludes=" + String.join(":", exclusions);
        }
        if (!inclusions.isEmpty()) {
            opt += ",includes=" + String.join(":", inclusions);
        }
        return opt;
    }

    private static String toVMName(final String srcName) {
        return srcName.replace('.', '/');
    }
}
