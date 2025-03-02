package edu.neu.ccs.prl.meringue;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICoverageVisitor;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.internal.data.CRC64;
import org.jacoco.core.internal.instr.InstrSupport;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.ISourceFileLocator;
import org.jacoco.report.MultiSourceFileLocator;
import org.objectweb.asm.ClassReader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CoverageCalculator {
    private static final int TAB_WIDTH = 4;
    final CoverageFilter filter;
    private final Map<Long, byte[]> idBufferMap = new HashMap<>();
    private final File temporaryDirectory;
    private long totalBranches = 0;

    CoverageCalculator(File temporaryDirectory, CoverageFilter filter) throws IOException {
        this.temporaryDirectory = temporaryDirectory;
        this.filter = filter;
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new RecordingAnalyzer(new ExecutionDataStore(), builder);
        for (File artifact : filter.getIncludedArtifacts()) {
            analyzer.analyzeAll(artifact);
        }
        if (filter.includeJavaClassLibrary()) {
            File javaHome = filter.getJavaHome();
            File jmods = new File(javaHome, "jmods");
            if (jmods.isDirectory()) {
                visitModularJavaClassLibrary(analyzer, javaHome);
            } else {
                analyzer.analyzeAll(javaHome);
            }
        }
        for (IClassCoverage classCoverage : builder.getClasses()) {
            if (this.filter.filter(classCoverage.getName())) {
                totalBranches += classCoverage.getBranchCounter().getTotalCount();
            }
        }
    }

    public long getTotalBranches() {
        return totalBranches;
    }

    public long calculate(byte[] execData) throws IOException {
        ExecFileLoader loader = new ExecFileLoader();
        loader.load(new ByteArrayInputStream(execData));
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new RecordingAnalyzer(loader.getExecutionDataStore(), builder);
        for (ExecutionData data : loader.getExecutionDataStore().getContents()) {
            if (filter.filter(data.getName()) && data.hasHits() && idBufferMap.containsKey(data.getId())) {
                analyzer.analyzeClass(idBufferMap.get(data.getId()), "");
            }
        }
        long hitBranches = 0;
        for (IClassCoverage classCoverage : builder.getClasses()) {
            hitBranches += classCoverage.getBranchCounter().getCoveredCount();
        }
        return hitBranches;
    }

    public void createReport(byte[] execData, String testDescription, JacocoReportFormat format, File directory)
            throws IOException {
        createReport(execData == null ? new byte[0] : execData, testDescription, format.createVisitor(directory),
                     format.shouldIncludeSources());
    }

    public void createReport(byte[] execData, String testDescription, IReportVisitor visitor,
                             boolean includeSources) throws IOException {
        ExecFileLoader loader = new ExecFileLoader();
        loader.load(new ByteArrayInputStream(execData));
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new RecordingAnalyzer(loader.getExecutionDataStore(), builder);
        for (Long key : idBufferMap.keySet()) {
            analyzer.analyzeClass(idBufferMap.get(key), "");
        }
        visitor.visitInfo(loader.getSessionInfoStore().getInfos(), loader.getExecutionDataStore().getContents());
        ISourceFileLocator locator = includeSources ? createLocator(filter.getIncludedArtifactSources()) :
                new MultiSourceFileLocator(TAB_WIDTH);
        visitor.visitBundle(builder.getBundle(testDescription), locator);
        visitor.visitEnd();
    }

    private ISourceFileLocator createLocator(Collection<File> sources) throws IOException {
        int i = 0;
        MultiSourceFileLocator locator = new MultiSourceFileLocator(TAB_WIDTH);
        for (File source : sources) {
            String name = source.getName();
            if (isArchive(name)) {
                File dest = new File(temporaryDirectory, name.substring(0, name.lastIndexOf(".")) + i++);
                if (!dest.exists()) {
                    extractArchive(source, dest.toPath());
                }
                locator.add(new DirectorySourceFileLocator(dest, "utf-8", TAB_WIDTH));
            } else {
                locator.add(new DirectorySourceFileLocator(source, "utf-8", TAB_WIDTH));
            }
        }
        return locator;
    }

    public CoverageFilter getFilter() {
        return filter;
    }

    private static boolean isArchive(String name) {
        return name.endsWith(".jar") || name.endsWith(".war") || name.endsWith(".zip");
    }

    private static void extractArchive(File source, Path destination) throws IOException {
        Files.createDirectories(destination);
        try (ZipFile archive = new ZipFile(source)) {
            // Sort the entries to ensure that parents are created before children
            List<? extends ZipEntry> entries =
                    archive.stream().sorted(Comparator.comparing(ZipEntry::getName)).collect(Collectors.toList());
            for (ZipEntry entry : entries) {
                Path entryDest = destination.resolve(entry.getName());
                if (entry.isDirectory()) {
                    FileUtil.ensureDirectory(entryDest.toFile());
                } else {
                    FileUtil.ensureDirectory(entryDest.getParent().toFile());
                    Files.copy(archive.getInputStream(entry), entryDest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void visitModularJavaClassLibrary(Analyzer analyzer, File javaHome) throws IOException {
        Path p = new File(javaHome, "lib" + File.separator + "jrt-fs.jar").toPath();
        if (Files.exists(p)) {
            try (URLClassLoader loader = new URLClassLoader(new URL[]{p.toUri().toURL()})) {
                try (FileSystem fs = FileSystems.newFileSystem(URI.create("jrt:/"),
                                                               Collections.emptyMap(),
                                                               loader)) {
                    Files.walkFileTree(fs.getPath("/modules"), new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            try (InputStream stream = Files.newInputStream(file)) {
                                analyzer.analyzeAll(stream, file.toString());
                                return FileVisitResult.CONTINUE;
                            }
                        }
                    });
                }
            }
        }
    }

    private static class SuppressingAnalyzer extends Analyzer {
        public SuppressingAnalyzer(ExecutionDataStore executionData, ICoverageVisitor coverageVisitor) {
            super(executionData, coverageVisitor);
        }

        @Override
        public void analyzeClass(final byte[] buffer, final String location) {
            try {
                super.analyzeClass(buffer, location);
            } catch (IOException | IllegalArgumentException e) {
                // Suppress the exception so that other classes are still analyzed
            }
        }

        @Override
        public int analyzeAll(final File file) throws IOException {
            if (file.isDirectory()) {
                return super.analyzeAll(file);
            } else {
                try (InputStream in = Files.newInputStream(file.toPath())) {
                    return analyzeAll(in, file.getPath());
                } catch (IOException | IllegalArgumentException e) {
                    // Suppress the exception so that other classes are still analyzed
                    return 0;
                }
            }
        }
    }

    private class RecordingAnalyzer extends SuppressingAnalyzer {
        public RecordingAnalyzer(ExecutionDataStore executionData, ICoverageVisitor coverageVisitor) {
            super(executionData, coverageVisitor);
        }

        @Override
        public void analyzeClass(final byte[] buffer, final String location) {
            final ClassReader reader = InstrSupport.classReaderFor(buffer);
            if (filter.filter(reader.getClassName())) {
                long classId = CRC64.classId(buffer);
                idBufferMap.put(classId, buffer);
                super.analyzeClass(buffer, location);
            }
        }
    }
}
