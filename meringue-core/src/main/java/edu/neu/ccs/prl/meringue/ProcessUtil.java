package edu.neu.ccs.prl.meringue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class ProcessUtil {
    /**
     * True if this JVM is being running on a Windows operating system.
     */
    private static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");
    /**
     * File representing the null device.
     */
    private static final File NULL_FILE = new File(IS_WINDOWS ? "NUL" : "/dev/null");
    /**
     * A redirection of subprocess output that writes the output to the null device, thereby discarding it.
     */
    private static final ProcessBuilder.Redirect DISCARD = ProcessBuilder.Redirect.to(NULL_FILE);

    private ProcessUtil() {
        throw new AssertionError(getClass().getSimpleName() + " is a static utility class and should " +
                                         "not be instantiated");
    }

    /**
     * Waits until the specified process has terminated or the specified waiting time has elapsed and then stops the
     * specified process if it has not yet terminated.
     *
     * @param timeout the maximum amount of time to wait
     * @param unit    the time unit of the timeout argument
     * @return true if the specified process exits and false if the waiting time elapsed before the process exited
     * @throws NullPointerException if the specified process is null
     * @throws InterruptedException if a thread interrupts this thread while it is waiting for the forked process to
     *                              finish
     */
    public static boolean waitFor(Process process, long timeout, TimeUnit unit) throws InterruptedException {
        try {
            return process.waitFor(timeout, unit);
        } finally {
            stop(process);
        }
    }

    /**
     * Waits until the specified process has terminated.
     *
     * @return the exit value of the process.
     * @throws NullPointerException if the specified process is null
     * @throws InterruptedException if a thread interrupts this thread while it is waiting for the process to finish
     */
    public static int waitFor(Process process) throws InterruptedException {
        try {
            return process.waitFor();
        } finally {
            stop(process);
        }
    }

    /**
     * Starts the process created by the specified builder.
     *
     * @param verbose true if the standard output and error of the forked process should be redirected to the standard
     *                out and error of this process instead of discarded
     * @param builder the builder used to create the process
     * @return the created process
     * @throws NullPointerException      if the specified builder is null or if an element of the command list of the
     *                                   specified builder is null
     * @throws IndexOutOfBoundsException if the command list of the specified builder is empty
     * @throws SecurityException         see {@link ProcessBuilder#start}
     * @throws IOException               if an I/O error occurs
     */
    public static Process start(ProcessBuilder builder, boolean verbose) throws IOException {
        return (verbose ? builder.inheritIO() : builder.redirectError(DISCARD).redirectOutput(DISCARD)).start();
    }

    public static void stop(Process process) throws InterruptedException {
        if (process.isAlive()) {
            process.destroy();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly().waitFor();
            }
        }
    }
}
