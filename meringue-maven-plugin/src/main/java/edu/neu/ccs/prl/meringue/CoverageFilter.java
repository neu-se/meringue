package edu.neu.ccs.prl.meringue;

import org.jacoco.agent.rt.internal_3570298.PreMain;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class CoverageFilter {
    /**
     * A list of class files to include in coverage reports. May use wildcard
     * characters (* and ?). By default, all files are included.
     * <p>
     * Non-null.
     */
    private final List<String> inclusions;
    /**
     * A list of class files to exclude from coverage reports. May use wildcard
     * characters (* and ?). By default, no files are excluded.
     * <p>
     * Non-null.
     */
    private final List<String> exclusions;
    /**
     * Set of JARs, directories, and files to be included in coverage reports.
     * <p>
     * Non-null.
     */
    private final Set<File> includedClasspathElements;

    public CoverageFilter(Collection<String> inclusions, Collection<String> exclusions,
                          Collection<File> includedClasspathElements) {
        this.inclusions = inclusions == null ? Collections.emptyList() : new ArrayList<>(inclusions);
        this.exclusions = exclusions == null ? Collections.emptyList() : new ArrayList<>(exclusions);
        if (includedClasspathElements == null) {
            this.includedClasspathElements = Collections.emptySet();
        } else {
            this.includedClasspathElements = new HashSet<>(includedClasspathElements);
        }
    }

    public CoverageCalculator createCoverageCalculator(Collection<File> classPathElements) throws IOException {
        try {
            return new CoverageCalculator(
                    includedClasspathElements.isEmpty() ? new HashSet<>(classPathElements) : includedClasspathElements,
                    inclusions.isEmpty() ? "*" : String.join(":", inclusions),
                    String.join(":", exclusions)
            );
        } catch (IOException e) {
            throw new IOException("Failed to initialize JaCoCo coverage calculate", e);
        }
    }

    public String getJacocoOption() {
        String opt = String.format("-javaagent:%s=output=none", FileUtil.getClassPathElement(PreMain.class));
        if (!exclusions.isEmpty()) {
            opt += ",excludes=" + String.join(":", exclusions);
        }
        if (!inclusions.isEmpty()) {
            opt += ",includes=" + String.join(":", inclusions);
        }
        return opt;
    }
}
