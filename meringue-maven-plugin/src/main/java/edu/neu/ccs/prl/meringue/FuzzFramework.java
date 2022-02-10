package edu.neu.ccs.prl.meringue;

import edu.neu.ccs.prl.meringue.internal.Replayer;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

public interface FuzzFramework {
    Process createCampaignProcess(CampaignConfiguration config, Duration duration, Properties options)
            throws IOException;

    File[] getCorpusFiles(CampaignConfiguration config, Properties options);

    File[] getFailureFiles(CampaignConfiguration config, Properties options);

    Class<? extends Replayer> getReplayerClass(CampaignConfiguration config, Properties options);
}