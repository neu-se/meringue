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
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.xml.XMLFormatter;
import org.jacoco.report.csv.CSVFormatter;
import org.objectweb.asm.ClassReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public enum ReportFormat {
    HTML () {
        @Override
        IReportVisitor createVisitor (File outputDir) throws IOException {
            return new HTMLFormatter().createVisitor(new FileMultiReportOutput(outputDir));
        }
    },
    CSV () {
        @Override
        IReportVisitor createVisitor (File outputDir) throws IOException {
            return new CSVFormatter().createVisitor(new FileOutputStream(
                                                    new File(outputDir, "jacoco.csv")));
        }
    },
    XML () {
        @Override
        IReportVisitor createVisitor (File outputDir) throws IOException {
            return new XMLFormatter().createVisitor(new FileOutputStream(
                                                    new File(outputDir, "jacoco.xml")));
        }
    };

    abstract IReportVisitor createVisitor (File outputDir) throws IOException; 
}
