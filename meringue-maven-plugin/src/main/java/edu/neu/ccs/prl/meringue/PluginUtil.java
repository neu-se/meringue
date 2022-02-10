package edu.neu.ccs.prl.meringue;

import org.apache.maven.surefire.booter.SystemUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public final class PluginUtil {
    private PluginUtil() {
        throw new AssertionError(getClass().getSimpleName() + " is a static utility class and should " +
                "not be instantiated");
    }

    /**
     * Creates a Java Archive (JAR) file containing only a manifest that specifies values for the Main-Class and
     * Class-Path attributes.
     *
     * @param classPathFiles set of files to be included on the class path
     * @param mainClass      the class containing the entry point method or null if the Main-Class attribute should not
     *                       be specified
     * @param dir            the directory in which the JAR file should be created or null if the default temporary
     *                       directory should be used
     * @return the created JAR file
     * @throws IOException          if an I/O error occurred
     * @throws NullPointerException if classPathFiles is null or an element of classPathFiles is null
     */
    public static File buildManifestJar(Set<File> classPathFiles, Class<?> mainClass, File dir) throws IOException {
        Set<File> classPathFilesCopy = new HashSet<>(classPathFiles);
        String[] paths = classPathFilesCopy.stream()
                .map(f -> f.isFile() ? f.getAbsolutePath() : f.getAbsolutePath() + "/")
                .toArray(String[]::new);
        File result = File.createTempFile("manifest", ".jar", dir);
        ensureDirectory(result.getParentFile());
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Class-Path", String.join(" ", paths));
        if (mainClass != null) {
            manifest.getMainAttributes().putValue("Main-Class", mainClass.getName());
        }
        JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(result)), manifest);
        jos.close();
        return result;
    }

    public static List<File> collectFiles(File directory, String extension) throws IOException {
        List<File> reports = new LinkedList<>();
        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                if (hasExtension(file.toFile(), extension)) {
                    reports.add(file.toFile());
                }
                return super.visitFile(file, attributes);
            }
        });
        return reports;
    }

    public static File getClassPathElement(Class<?> clazz) {
        return new File(clazz.getProtectionDomain().getCodeSource().getLocation().getPath());
    }

    public static File javaHomeToJavaExec(File javaHome) {
        return new File(javaHome, "bin" + File.separator + "java");
    }

    public static File javaExecToJavaHome(File javaExec) {
        return SystemUtils.toJdkHomeFromJvmExec(javaExec.getPath());
    }

    public static boolean isInvalidJavaExecutable(File javaExec) {
        if (!SystemUtils.endsWithJavaPath(javaExec.getAbsolutePath())) {
            return true;
        }
        if (javaExec.isFile()) {
            return false;
        } else {
            return !"java".equals(javaExec.getName()) || !javaExec.getParentFile().isDirectory();
        }
    }

    public static boolean hasExtension(File file, String targetExtension) {
        return hasExtension(file) && targetExtension.equals(getExtension(file));
    }

    public static boolean hasExtension(File file) {
        return file.isFile() && file.getName().contains(".");
    }

    public static String getExtension(File file) {
        if (!hasExtension(file)) {
            throw new IllegalArgumentException();
        }
        String name = file.getName();
        return name.substring(name.lastIndexOf(".") + 1);
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
    public static void createOrCleanDirectory(File dir) throws IOException {
        if (dir.isDirectory()) {
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
        } else if (dir.isFile() && !dir.delete()) {
            throw new IOException("Failed to delete: " + dir);
        }
        if (!dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + dir);
        }
    }
}
