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
        // Open the loopback connection
        try (ForkConnection connection = new ForkConnection(Integer.parseInt(args[0]))) {
            String testClassName = connection.receive(String.class);
            String testMethodName = connection.receive(String.class);
            String replayerClassName = connection.receive(String.class);
            File[] inputFiles = connection.receive(File[].class);
            StackTraceCleaner cleaner = connection.receive(StackTraceCleaner.class);
            Replayer replayer = (Replayer) Class.forName(replayerClassName).getDeclaredConstructor().newInstance();
            replayer.configure(testClassName, testMethodName, AnalysisForkMain.class.getClassLoader());
            // Reset the JaCoCo coverage
            RT.getAgent().reset();
            // Run the inputs
            for (File inputFile : inputFiles) {
                // Read the input
                byte[] input = Files.readAllBytes(inputFile.toPath());
                // Execute the test
                Throwable result = replayer.execute(input);
                // Send current JaCoCo coverage without resetting the coverage
                connection.send(RT.getAgent().getExecutionData(false));
                if (result == null) {
                    connection.send(false);
                } else {
                    connection.send(true);
                    connection.send(cleaner.cleanStackTrace(result).toArray(new StackTraceElement[0]));
                }
            }
            // Wait for a shutdown signal
            connection.receive(Object.class);
        }
    }
}