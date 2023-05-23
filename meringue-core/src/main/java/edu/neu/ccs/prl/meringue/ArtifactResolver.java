package edu.neu.ccs.prl.meringue;

import java.io.File;
import java.util.Collection;
import java.util.List;

public interface ArtifactResolver {
    List<File> resolve(String groupId, String artifactId, String version, String type,
                              String classifier, boolean transitive);

    String buildClassPath(Collection<File> elements);
}
