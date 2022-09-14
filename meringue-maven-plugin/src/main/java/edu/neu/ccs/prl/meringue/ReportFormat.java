package edu.neu.ccs.prl.meringue;

import org.jacoco.report.*;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.xml.XMLFormatter;
import org.jacoco.report.csv.CSVFormatter;

import java.io.*;

public enum ReportFormat {
    HTML() {
        @Override
        IReportVisitor createVisitor(File outputDir) throws IOException {
            return new HTMLFormatter().createVisitor(new FileMultiReportOutput(outputDir));
        }
    },
    CSV() {
        @Override
        IReportVisitor createVisitor(File outputDir) throws IOException {
            return new CSVFormatter().createVisitor(new FileOutputStream(
                                                    new File(outputDir, "jacoco.csv")));
        }
    },
    XML() {
        @Override
        IReportVisitor createVisitor(File outputDir) throws IOException {
            return new XMLFormatter().createVisitor(new FileOutputStream(
                                                    new File(outputDir, "jacoco.xml")));
        }
    };

    abstract IReportVisitor createVisitor(File outputDir) throws IOException;
}
