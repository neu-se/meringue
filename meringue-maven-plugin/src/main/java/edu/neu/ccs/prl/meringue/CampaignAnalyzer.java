package edu.neu.ccs.prl.meringue;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

final class CampaignAnalyzer implements Closeable {
    private final JvmLauncher launcher;
    private final CampaignReport report;
    private final ServerSocket server;
    private ForkConnection connection;
    private Process process;

    CampaignAnalyzer(JvmLauncher launcher, CampaignReport report) throws IOException {
        this.report = report;
        // Create a server socket bound to an automatically allocated port
        this.server = new ServerSocket(0);
        this.launcher = launcher.appendArguments(String.valueOf(server.getLocalPort()));
    }

    CampaignReport getReport() {
        return report;
    }

    private void restartConnection() throws IOException {
        closeConnection();
        // Launch the analysis JVM
        this.process = launcher.launch();
        // Connection to the JVM
        this.connection = new ForkConnection(server.accept());
    }

    private boolean isConnected() {
        return connection != null && !connection.isClosed();
    }

    void analyze(File inputFile) throws IOException, ClassNotFoundException {
        if (!isConnected()) {
            restartConnection();
        }
        try {
            connection.send(inputFile);
            byte[] execData = connection.receive(byte[].class);
            StackTraceElement[] trace = null;
            if (connection.receive(Boolean.class)) {
                trace = connection.receive(StackTraceElement[].class);
            }
            report.record(inputFile, execData, trace);
        } catch (Throwable t) {
            // Input caused forked to fail
            closeConnection();
            restartConnection();
            report.recordJvmCrash(inputFile);
        }
    }

    private void closeConnection() {
        if (connection != null && !connection.isClosed()) {
            try {
                connection.send(null);
            } catch (IOException e) {
                // Failed to send shutdown signal
            }
            connection.close();
            connection = null;
        }
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                //
            }
            process = null;
        }
    }

    @Override
    public void close() throws IOException {
        server.close();
        closeConnection();
    }
}
