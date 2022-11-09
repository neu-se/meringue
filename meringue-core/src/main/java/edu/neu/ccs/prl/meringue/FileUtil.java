package edu.neu.ccs.prl.meringue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public final class FileUtil {
    private FileUtil() {
        throw new AssertionError();
    }

    /**
     * Creates a Java Archive (JAR) file containing only a manifest that specifies a value for the Class-Path
     * attribute.
     *
     * @param classpathElements elements to be included on the classpath
     * @param jar               the JAR file that should be created
     * @throws IOException          if an I/O error occurs
     * @throws NullPointerException if {@code classpathElements} is null, an element of {@code classpathElements} is
     *                              null, or {@code jar} is null.
     */
    public static void buildManifestJar(Collection<File> classpathElements, File jar) throws IOException {
        Set<File> classPathFilesCopy = new HashSet<>(classpathElements);
        String[] paths = classPathFilesCopy.stream()
                                           .map(f -> f.isFile() ? f.getAbsolutePath() : f.getAbsolutePath() + "/")
                                           .toArray(String[]::new);
        ensureDirectory(jar.getParentFile());
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Class-Path", String.join(" ", paths));
        JarOutputStream jos =
                new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(jar.toPath())), manifest);
        jos.close();
    }

    public static File getClassPathElement(Class<?> clazz) {
        return new File(clazz.getProtectionDomain().getCodeSource().getLocation().getPath());
    }

    public static File javaHomeToJavaExec(File javaHome) {
        return new File(javaHome, "bin" + File.separator + "java");
    }

    /**
     * Returns the home directory of the Java installation for the specified Java executable. pre-java 9
     * HOME/jre/bin/java or HOME/bin/java
     *
     * @param javaExec a Java executable
     * @return the home directory of the Java installation for the specified Java executable
     * @throws NullPointerException     if {@code javaExec} is null
     * @throws IllegalArgumentException if {@code javaExec} does not point to a Java executable
     */
    public static File javaExecToJavaHome(File javaExec) {
        if ("java".equals(javaExec.getName())) {
            File parent = javaExec.getParentFile();
            if ("bin".equals(parent.getName())) {
                File grandparent = parent.getParentFile();
                return "jre".equals(grandparent.getName()) ? grandparent.getParentFile() : grandparent;
            }
        }
        throw new IllegalArgumentException("Invalid Java executable " + javaExec);
    }

    /**
     * Creates the specified directory if it does not already exist.
     *
     * @param dir the directory to create
     * @throws IOException if the specified directory did not already exist and was not successfully created
     */
    public static void ensureDirectory(File dir) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + dir);
        }
    }

    /**
     * Ensures that the specified directory exists and is empty.
     *
     * @param dir the directory to be created or emptied
     * @throws IOException if the specified directory could not be created or emptied
     */
    public static void ensureEmptyDirectory(File dir) throws IOException {
        if (dir.isDirectory()) {
            deleteDirectory(dir);
        } else {
            delete(dir);
        }
        ensureDirectory(dir);
    }

    /**
     * Deletes the specified directory and all of its contents.
     *
     * @param dir the directory to be deleted
     * @throws NullPointerException if the specified directory is {@code null}
     * @throws IOException          if {@code dir} is not a directory or an I/O error occurs
     */
    public static void deleteDirectory(File dir) throws IOException {
        if (!dir.isDirectory()) {
            throw new IOException();
        }
        Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                if (e == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw e;
                }
            }
        });
    }

    public static void delete(File file) throws IOException {
        if (file.exists() && !file.delete()) {
            throw new IOException("Failed to delete existing file: " + file);
        }
    }
}
