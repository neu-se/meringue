package edu.neu.ccs.prl.meringue;

import java.io.File;

public interface JarFuzzFramework extends FuzzFramework {
    String getCoordinate();
    void setFrameworkJar(File frameworkJar);
}
