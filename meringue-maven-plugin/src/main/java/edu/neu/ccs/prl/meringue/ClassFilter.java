package edu.neu.ccs.prl.meringue;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.*;

/**
 * Filters classes. Immutable.
 */
public final class ClassFilter {
    private final List<String> inclusions;
    private final List<String> exclusions;
    private final Set<String> includedArtifacts;

    public ClassFilter(Collection<String> inclusions, Collection<String> exclusions,
                       Collection<String> includedArtifacts) {
        this.inclusions = inclusions == null ? Collections.emptyList() :
                Collections.unmodifiableList(new ArrayList<>(inclusions));
        this.exclusions = exclusions == null ? Collections.emptyList() :
                Collections.unmodifiableList(new ArrayList<>(exclusions));
        this.includedArtifacts =
                includedArtifacts == null ? Collections.emptySet() :
                        Collections.unmodifiableSet(new HashSet<>(includedArtifacts));
    }

    public ClassFilter(AnalysisValues values) throws MojoExecutionException {
        this(values.getInclusions(), values.getExclusions(), values.getIncludedArtifacts());
    }

    public String getExcludeString() {
        return String.join(":", this.exclusions);
    }

    public String getIncludeString() {
        return String.join(":", this.inclusions);
    }

    public boolean isIncludedArtifact(Artifact artifact) {
        return isIncludedArtifact(artifact.getGroupId(), artifact.getArtifactId());
    }

    public boolean isIncludedArtifact(String groupId, String artifactId) {
        return includedArtifacts.isEmpty() || includedArtifacts.contains(groupId + ":" + artifactId) ||
                includedArtifacts.contains(groupId + ":" + "*");
    }
}
