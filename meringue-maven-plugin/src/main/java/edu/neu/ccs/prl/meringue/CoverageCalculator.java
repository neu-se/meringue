package edu.neu.ccs.prl.meringue;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICoverageVisitor;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.internal.data.CRC64;
import org.jacoco.core.internal.instr.InstrSupport;
import org.jacoco.core.runtime.WildcardMatcher;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.*;
import org.objectweb.asm.ClassReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class CoverageCalculator {
    private final Map<Long, byte[]> idBufferMap = new HashMap<>();
    private final WildcardMatcher includes;
    private final WildcardMatcher excludes;
    private long totalBranches = 0;

    CoverageCalculator(Collection<File> classPathElements, String includes, String excludes) throws IOException {
        this.includes = new WildcardMatcher(toVMName(includes));
        this.excludes = new WildcardMatcher(toVMName(excludes));
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new RecordingAnalyzer(new ExecutionDataStore(), builder);
        for (File classpathElement : classPathElements) {
            analyzer.analyzeAll(classpathElement);
        }
        for (IClassCoverage classCoverage : builder.getClasses()) {
            if (filter(classCoverage.getName())) {
                totalBranches += classCoverage.getBranchCounter().getTotalCount();
            }
        }
    }

    public long getTotalBranches() {
        return totalBranches;
    }

    boolean filter(String className) {
        return includes.matches(className) && !excludes.matches(className);
    }

    public long calculate(byte[] execData) throws IOException {
        ExecFileLoader loader = new ExecFileLoader();
        loader.load(new ByteArrayInputStream(execData));
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new RecordingAnalyzer(loader.getExecutionDataStore(), builder);
        for (ExecutionData data : loader.getExecutionDataStore().getContents()) {
            if (filter(data.getName()) && data.hasHits() && idBufferMap.containsKey(data.getId())) {
                analyzer.analyzeClass(idBufferMap.get(data.getId()), "");
            }
        }
        long hitBranches = 0;
        for (IClassCoverage classCoverage : builder.getClasses()) {
            hitBranches += classCoverage.getBranchCounter().getCoveredCount();
        }
        return hitBranches;
    }

    public void createReport(byte[] execData, String testDesc, File[] sources, IReportVisitor visitor)
            throws IOException {
        ExecFileLoader loader = new ExecFileLoader();
        loader.load(new ByteArrayInputStream(execData));
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new RecordingAnalyzer(loader.getExecutionDataStore(), builder);
        for (Long key : idBufferMap.keySet()) {
            analyzer.analyzeClass(idBufferMap.get(key), "");
        }
        visitor.visitInfo(loader.getSessionInfoStore().getInfos(), loader.getExecutionDataStore().getContents());
        visitor.visitBundle(builder.getBundle(testDesc), createLocator(sources));
        visitor.visitEnd();
    }

    private static ISourceFileLocator createLocator(File[] sources) throws IOException {
        MultiSourceFileLocator locator = new MultiSourceFileLocator(4);
        for (File source : sources) {
            String name = source.getName();
            if (isArchive(name)) {
                File dest = new File(source.getParent(), name.substring(0, name.lastIndexOf(".")));
                if (!dest.exists()) {
                    extractArchive(source, dest.toPath());
                    locator.add(new DirectorySourceFileLocator(dest, "utf-8", 4));
                }
            } else {
                locator.add(new DirectorySourceFileLocator(source, "utf-8", 4));
            }
        }
        return locator;
    }

    private static String toVMName(final String srcName) {
        return srcName.replace('.', '/');
    }

    private static boolean isArchive(String name) {
        return name.endsWith(".jar") || name.endsWith(".war") || name.endsWith(".zip");
    }

    private static void extractArchive(File source, Path destination) throws IOException {
        Files.createDirectories(destination);
        try (ZipFile archive = new ZipFile(source)) {
            // Sort the entries to ensure that parents are created before children
            List<? extends ZipEntry> entries = archive.stream()
                    .sorted(Comparator.comparing(ZipEntry::getName))
                    .collect(Collectors.toList());
            for (ZipEntry entry : entries) {
                Path entryDest = destination.resolve(entry.getName());
                if (entry.isDirectory()) {
                    FileUtil.ensureDirectory(entryDest.toFile());
                } else {
                    FileUtil.ensureDirectory(entryDest.getParent().toFile());
                    Files.copy(archive.getInputStream(entry), entryDest);
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
            } catch (IOException e) {
                // Suppress the exception so that other classes are still analyzed
            }
        }

        @Override
        public int analyzeAll(final File file) throws IOException {
            if (file.isDirectory()) {
                return super.analyzeAll(file);
            } else {
                try (InputStream in = new FileInputStream(file)) {
                    return analyzeAll(in, file.getPath());
                } catch (IOException e) {
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
            if (filter(reader.getClassName())) {
                long classId = CRC64.classId(buffer);
                idBufferMap.put(classId, buffer);
                super.analyzeClass(buffer, location);
            }
        }
    }
}
