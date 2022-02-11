package edu.neu.ccs.prl.meringue;

public final class CampaignForkMain {
    private CampaignForkMain() {
        throw new AssertionError(getClass().getSimpleName() + " is a static utility class and should " +
                "not be instantiated");
    }

    public static void main(String[] args) throws Throwable {
        // Open the loopback connection
        try (ForkConnection connection = new ForkConnection(Integer.parseInt(args[0]))) {
            CampaignConfiguration config = connection.receive(CampaignConfiguration.class);
            // TODO
        }
    }
}
