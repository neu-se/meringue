package edu.neu.ccs.prl.meringue;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Classes implementing {@link FuzzFramework} must declare a public, zero-argument constructor.
 */
public interface FuzzFramework {
    void initialize(CampaignConfiguration config, Properties frameworkOptions);

    Process startCampaign() throws IOException;

    File[] getCorpusFiles();

    File[] getFailureFiles();

    Class<? extends Replayer> getReplayerClass();

    File[] getFrameworkClassPathElements();
}