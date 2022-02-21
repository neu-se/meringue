package edu.neu.ccs.prl.meringue;

import org.jacoco.agent.rt.RT;

import java.io.File;
import java.nio.file.Files;

public final class AnalysisForkMain {
    private AnalysisForkMain() {
        throw new AssertionError(getClass().getSimpleName() + " is a static utility class and should " +
                "not be instantiated");
    }

    public static void main(String[] args) throws Throwable {
        String testClassName = args[0];
        String testMethodName = args[1];
        String replayerClassName = args[2];
        int maxTraceSize = Integer.parseInt(args[3]);
        int port = Integer.parseInt(args[4]);
        StackTraceCleaner cleaner = new StackTraceCleaner(maxTraceSize);
        Replayer replayer = (Replayer) Class.forName(replayerClassName)
                .getDeclaredConstructor()
                .newInstance();
        replayer.configure(testClassName, testMethodName, AnalysisForkMain.class.getClassLoader());
        // Open the loopback connection
        try (ForkConnection connection = new ForkConnection(port)) {
            for(;;) {
                File inputFile = connection.receive(File.class);
                if(inputFile == null) {
                    return;
                }
                // Read the input
                byte[] input = Files.readAllBytes(inputFile.toPath());
                // Reset the JaCoCo coverage
                RT.getAgent().reset();
                // Execute the test
                Throwable result = replayer.execute(input);
                // Send current JaCoCo coverage
                connection.send(RT.getAgent().getExecutionData(false));
                if (result == null) {
                    connection.send(false);
                } else {
                    connection.send(true);
                    connection.send(cleaner.cleanStackTrace(result).toArray(new StackTraceElement[0]));
                }
            }
        }
    }
}