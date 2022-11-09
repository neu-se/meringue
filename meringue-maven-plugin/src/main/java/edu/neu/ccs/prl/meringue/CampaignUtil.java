package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.surefire.SurefireHelper;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class CampaignUtil {
    private CampaignUtil() {
        throw new AssertionError();
    }

    public static String buildClassPath(File... classPathElements) {
        return Arrays.stream(classPathElements)
                     .map(File::getAbsolutePath)
                     .map(SurefireHelper::escapeToPlatformPath)
                     .collect(Collectors.joining(File.pathSeparator));
    }
}
