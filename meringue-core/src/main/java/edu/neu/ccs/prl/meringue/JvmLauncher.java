package edu.neu.ccs.prl.meringue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public final class JvmLauncher implements Serializable {
    /**
     * Prefix of JVM option indicating that the JVM should suspend and wait for a debugger to attach.
     */
    public static final String DEBUG_OPT = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=127.0.0.1:";
    private static final long serialVersionUID = -2657754668579290763L;
    private final File javaExec;
    private final String[] options;
    /**
     * {@code true} if the standard output and error of the forked JVM should be redirected to the standard out and
     * error of this process instead of discarded
     */
    private final boolean verbose;
    private final String[] arguments;
    private final File workingDir;
    private final Map<String, String> environment;
    private final BiFunction<JvmLauncher, String[], String[]> commandCreator;

    JvmLauncher(File javaExec, String[] options, boolean verbose, String[] arguments, File workingDir,
                Map<String, String> environment, BiFunction<JvmLauncher, String[], String[]> commandCreator) {
        if (!javaExec.isFile() || (workingDir != null && !workingDir.isDirectory())) {
            throw new IllegalArgumentException();
        }
        if (commandCreator == null) {
            throw new NullPointerException();
        }
        this.workingDir = workingDir;
        this.environment = environment == null ? null : Collections.unmodifiableMap(new HashMap<>(environment));
        this.javaExec = javaExec;
        this.options = options.clone();
        this.verbose = verbose;
        this.arguments = arguments.clone();
        JvmLauncher.checkElementsNonNull(this.arguments);
        JvmLauncher.checkElementsNonNull(this.options);
        this.commandCreator = commandCreator;
    }

    /**
     * Launches a new forked JVM.
     *
     * @throws SecurityException see {@link ProcessBuilder#start}
     * @throws IOException       if an I/O error occurs
     */
    public Process launch() throws IOException {
        ProcessBuilder builder = new ProcessBuilder().command(createCommand()).directory(workingDir);
        if (environment != null) {
            builder.environment().clear();
            builder.environment().putAll(environment);
        }
        return ProcessUtil.start(builder, isVerbose());
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

    public File getWorkingDir() {
        return workingDir;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public JvmLauncher withJavaExec(File javaExec) {
        return new JvmLauncher(javaExec, options, verbose, arguments, workingDir, environment, commandCreator);
    }

    public JvmLauncher withOptions(String... options) {
        return new JvmLauncher(javaExec, options, verbose, arguments, workingDir, environment, commandCreator);
    }

    public JvmLauncher withVerbose(boolean verbose) {
        return new JvmLauncher(javaExec, options, verbose, arguments, workingDir, environment, commandCreator);
    }

    public JvmLauncher withArguments(String... arguments) {
        return new JvmLauncher(javaExec, options, verbose, arguments, workingDir, environment, commandCreator);
    }

    public String[] createCommand() {
        return commandCreator.apply(this, arguments);
    }

    public String[] createCommand(String... arguments) {
        return commandCreator.apply(this, arguments);
    }

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

    public JvmLauncher withEnvironment(Map<String, String> environment) {
        return new JvmLauncher(javaExec, options, verbose, arguments, workingDir, environment, commandCreator);
    }

    public JvmLauncher withWorkingDirectory(File workingDir) {
        return new JvmLauncher(javaExec, options, verbose, arguments, workingDir, environment, commandCreator);
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
    public static JvmLauncher fromJar(File javaExec, File jar, String[] options, boolean verbose, String[] arguments,
                                      File workingDir, Map<String, String> environment) {
        return new JvmLauncher(javaExec, options, verbose, arguments, workingDir, environment,
                               new JarCommandCreator(jar));
    }

    /**
     * Creates forked JVMs using a command of the form: java [ options ] class [ argument ... ].
     */
    public static JvmLauncher fromMain(File javaExec, String mainClassName, String[] options, boolean verbose,
                                       String[] arguments, File workingDir, Map<String, String> environment) {
        return new JvmLauncher(javaExec, options, verbose, arguments, workingDir, environment,
                               new JavaMainCommandCreator(mainClassName));
    }

    private static final class JarCommandCreator implements BiFunction<JvmLauncher, String[], String[]>, Serializable {
        private static final long serialVersionUID = 4166136230103308077L;
        private final File jar;

        private JarCommandCreator(File jar) {
            this.jar = jar;
            if (!jar.isFile()) {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public String[] apply(JvmLauncher launcher, String[] strings) {
            String[] options = launcher.getOptions();
            String[] arguments = launcher.getArguments();
            String[] command = new String[options.length + arguments.length + 3];
            int i = 0;
            command[i++] = launcher.getJavaExec().getAbsolutePath();
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
    }

    private static final class JavaMainCommandCreator
            implements BiFunction<JvmLauncher, String[], String[]>, Serializable {
        private static final long serialVersionUID = 4166136230103308077L;
        private final String mainClassName;

        private JavaMainCommandCreator(String mainClassName) {
            if (mainClassName == null) {
                throw new NullPointerException();
            }
            this.mainClassName = mainClassName;
        }

        @Override
        public String[] apply(JvmLauncher launcher, String[] strings) {
            String[] options = launcher.getOptions();
            String[] arguments = launcher.getArguments();
            String[] command = new String[options.length + arguments.length + 2];
            int i = 0;
            command[i++] = launcher.getJavaExec().getAbsolutePath();
            for (String option : options) {
                command[i++] = option;
            }
            command[i++] = mainClassName;
            for (String argument : arguments) {
                command[i++] = argument;
            }
            return command;
        }
    }
}