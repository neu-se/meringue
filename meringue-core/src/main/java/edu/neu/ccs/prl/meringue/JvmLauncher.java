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

    /**
     * @param verbose true if the standard output and error of the forked JVM should be redirected to the standard
     *                out and error of this process instead of discarded
     */
    JvmLauncher(File javaExec, String[] options, boolean verbose, String[] arguments) {
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
    public Process launch() throws IOException {
        return ProcessUtil.start(new ProcessBuilder(createCommand()), isVerbose());
    }

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

    abstract JvmLauncher with(File javaExec, String[] options, boolean verbose, String[] arguments);

    public JvmLauncher withJavaExec(File javaExec) {
        return with(javaExec, options, verbose, arguments);
    }

    public JvmLauncher withOptions(String... options) {
        return with(javaExec, options, verbose, arguments);
    }

    public JvmLauncher withVerbose(boolean verbose) {
        return with(javaExec, options, verbose, arguments);
    }

    public JvmLauncher withArguments(String... arguments) {
        return with(javaExec, options, verbose, arguments);
    }

    public String[] createCommand() {
        return createCommand(arguments);
    }

    public abstract String[] createCommand(String... arguments);

    public JvmLauncher appendArguments(String... arguments) {
        String[] newArguments = new String[this.arguments.length + arguments.length];
        System.arraycopy(this.arguments, 0, newArguments, 0, this.arguments.length);
        System.arraycopy(arguments, 0, newArguments, this.arguments.length, arguments.length);
        return withArguments(newArguments);
    }

    public JvmLauncher appendOptions(String... options) {
        String[] newOptions = new String[this.options.length + options.length];
        System.arraycopy(this.options, 0, newOptions, 0, this.options.length);
        System.arraycopy(options, 0, newOptions, this.options.length, options.length);
        return withOptions(newOptions);
    }

    private static <T> void checkElementsNonNull(T[] elements) {
        for (T element : elements) {
            if (element == null) {
                throw new NullPointerException();
            }
        }
    }

    /**
     * Creates forked JVMs using a command of the form: java [ options ] -jar file.jar [ argument ... ].
     */
    public static final class JarLauncher extends JvmLauncher {
        private static final long serialVersionUID = -6897111153301141296L;
        private final File jar;

        public JarLauncher(File javaExec, File jar, String[] options, boolean verbose, String[] arguments) {
            super(javaExec, options, verbose, arguments);
            if (!jar.isFile()) {
                throw new IllegalArgumentException();
            }
            this.jar = jar;
        }

        @Override
        public String[] createCommand(String[] arguments) {
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
            return command;
        }

        @Override
        JarLauncher with(File javaExec, String[] options, boolean verbose, String[] arguments) {
            return new JarLauncher(javaExec, jar, options, verbose, arguments);
        }

        public File getJar() {
            return jar;
        }

        public JarLauncher withJar(File jar) {
            return new JarLauncher(getJavaExec(), jar, getOptions(), isVerbose(), getArguments());
        }
    }

    /**
     * Creates forked JVMs using a command of the form: java [ options ] class [ argument ... ].
     */
    public static final class JavaMainLauncher extends JvmLauncher {
        private static final long serialVersionUID = 6658540649100605982L;
        private final String mainClassName;

        public JavaMainLauncher(File javaExec, String mainClassName, String[] options, boolean verbose,
                                String[] arguments) {
            super(javaExec, options, verbose, arguments);
            if (mainClassName.isEmpty()) {
                throw new IllegalArgumentException();
            }
            this.mainClassName = mainClassName;
        }

        @Override
        public String[] createCommand(String[] arguments) {
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
            return command;
        }

        @Override
        JavaMainLauncher with(File javaExec, String[] options, boolean verbose, String[] arguments) {
            return new JavaMainLauncher(javaExec, mainClassName, options, verbose, arguments);
        }

        public String getMainClassName() {
            return mainClassName;
        }

        public JavaMainLauncher withMainClassName(String mainClassName) {
            return new JavaMainLauncher(getJavaExec(), mainClassName, getOptions(), isVerbose(), getArguments());
        }
    }
}