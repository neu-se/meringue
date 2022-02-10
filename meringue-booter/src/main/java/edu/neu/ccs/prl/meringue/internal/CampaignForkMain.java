package edu.neu.ccs.prl.meringue.internal;

import java.io.IOException;

public final class CampaignForkMain {
    private CampaignForkMain() {
        throw new AssertionError(getClass().getSimpleName() + " is a static utility class and should " +
                "not be instantiated");
    }

    public static void main(String[] args) throws IOException {
        // Open the loopback connection
        try (ForkConnection connection = new ForkConnection(Integer.parseInt(args[0]))) {
            // TODO
        }
    }
}
