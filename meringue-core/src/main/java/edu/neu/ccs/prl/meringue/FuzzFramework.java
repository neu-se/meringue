package edu.neu.ccs.prl.meringue;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Classes implementing {@link FuzzFramework} must declare a public, zero-argument constructor.
 */
public interface FuzzFramework {
    void initialize(CampaignConfiguration config, Properties frameworkArguments)
            throws IOException, ReflectiveOperationException;

    Process startCampaign() throws IOException;

    File[] getCorpusFiles() throws IOException;

    File[] getFailureFiles() throws IOException;

    Class<? extends Replayer> getReplayerClass() throws ReflectiveOperationException;

    File[] getFrameworkClassPathElements();
}