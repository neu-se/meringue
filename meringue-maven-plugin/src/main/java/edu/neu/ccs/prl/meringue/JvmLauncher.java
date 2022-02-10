package edu.neu.ccs.prl.meringue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public abstract class JvmLauncher implements Serializable {
    /**
     * Prefix of JVM option indicating that the JVM should suspend and wait for a debugger to attach.
     */
    public static final String DEBUG_OPT = "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=";
    private static final long serialVersionUID = -2657754668579290763L;
    private final File javaExec;
    private final String[] options;
    private final boolean verbose;
    private final String[] arguments;

    public JvmLauncher(File javaExec, String[] options, boolean verbose, String[] arguments) {
        if (!javaExec.isFile()) {
            throw new IllegalArgumentException();
        }
        this.javaExec = javaExec;
        this.options = options.clone();
        this.verbose = verbose;
        this.arguments = arguments.clone();
        JvmLauncher.checkElementsNonNull(this.arguments);
        JvmLauncher.checkElementsNonNull(this.options);
    }

    /**
     * Launches a new forked JVM.
     *
     * @throws SecurityException see {@link ProcessBuilder#start}
     * @throws IOException       if an I/O error occurs
     */
    public abstract Process launch() throws IOException;

    public abstract Process launch(String[] arguments) throws IOException;

    public File getJavaExec() {
        return javaExec;
    }

    public String[] getOptions() {
        return options.clone();
    }

    public boolean isVerbose() {
        return verbose;
    }

    public String[] getArguments() {
        return arguments.clone();
    }

    /**
     * Launches a forked JVM using a command of the form: java [ options ] -jar file.jar [ argument ... ].
     *
     * @param verbose true if the standard output and error of the forked JVM should be redirected to the standard
     *                out and error of this process instead of discarded
     * @throws NullPointerException if any argument is null
     * @throws SecurityException    see {@link ProcessBuilder#start}
     * @throws IOException          if an I/O error occurs
     */
    public static Process launchJvm(File javaExec, File jar, String[] options, boolean verbose, String... arguments)
            throws IOException {
        return new JarLauncher(javaExec, jar, options, verbose, arguments).launch();
    }

    /**
     * Launches a forked JVM using a command of the form: java [ options ] class [ argument ... ].
     *
     * @param verbose true if the standard output and error of the forked JVM should be redirected to the standard
     *                out and error of this process instead of discarded
     * @throws NullPointerException if any argument is null
     * @throws SecurityException    see {@link ProcessBuilder#start}
     * @throws IOException          if an I/O error occurs
     */
    public static Process launchJvm(File javaExec, String mainClassName, String[] options, boolean verbose,
                                    String... arguments) throws IOException {
        return new JavaMainLauncher(javaExec, mainClassName, options, verbose, arguments).launch();
    }

    private static <T> void checkElementsNonNull(T[] elements) {
        for (T element : elements) {
            if (element == null) {
                throw new NullPointerException();
            }
        }
    }

    /**
     * Creates forked JVMs using a command of the form: java [ options ] class [ argument ... ].
     */
    public static final class JarLauncher extends JvmLauncher {
        private static final long serialVersionUID = -6897111153301141296L;
        private final File jar;

        /**
         * @param verbose true if the standard output and error of the forked JVM should be redirected to the standard
         *                out and error of this process instead of discarded
         */
        public JarLauncher(File javaExec, File jar, String[] options, boolean verbose, String[] arguments) {
            super(javaExec, options, verbose, arguments);
            if (!jar.isFile()) {
                throw new IllegalArgumentException();
            }
            this.jar = jar;
        }

        @Override
        public Process launch() throws IOException {
            return launch(getArguments());
        }

        @Override
        public Process launch(String[] arguments) throws IOException {
            String[] options = getOptions();
            String[] command = new String[options.length + arguments.length + 3];
            int i = 0;
            command[i++] = getJavaExec().getAbsolutePath();
            for (String option : options) {
                command[i++] = option;
            }
            command[i++] = "-jar";
            command[i++] = jar.getAbsolutePath();
            for (String argument : arguments) {
                command[i++] = argument;
            }
            return ProcessUtil.start(new ProcessBuilder(command), isVerbose());
        }
    }

    /**
     * Creates forked JVMs using a command of the form: java [ options ] class [ argument ... ].
     */
    public static final class JavaMainLauncher extends JvmLauncher {
        private static final long serialVersionUID = 6658540649100605982L;
        private final String mainClassName;

        /**
         * @param verbose true if the standard output and error of the forked JVM should be redirected to the standard
         *                out and error of this process instead of discarded
         */
        public JavaMainLauncher(File javaExec, String mainClassName, String[] options, boolean verbose,
                                String[] arguments) {
            super(javaExec, options, verbose, arguments);
            if (mainClassName.isEmpty()) {
                throw new IllegalArgumentException();
            }
            this.mainClassName = mainClassName;
        }

        @Override
        public Process launch() throws IOException {
            return launch(getArguments());
        }

        @Override
        public Process launch(String[] arguments) throws IOException {
            String[] options = getOptions();
            String[] command = new String[options.length + arguments.length + 2];
            int i = 0;
            command[i++] = getJavaExec().getAbsolutePath();
            for (String option : options) {
                command[i++] = option;
            }
            command[i++] = mainClassName;
            for (String argument : arguments) {
                command[i++] = argument;
            }
            return ProcessUtil.start(new ProcessBuilder(command), isVerbose());
        }
    }
}