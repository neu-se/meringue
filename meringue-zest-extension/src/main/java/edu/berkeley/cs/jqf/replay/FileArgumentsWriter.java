package edu.berkeley.cs.jqf.replay;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public class FileArgumentsWriter implements ArgumentsWriter {
    private final File outputDir;

    public FileArgumentsWriter(File outputDir) throws IOException {
        this.outputDir = outputDir;
        if (!outputDir.isDirectory()) {
            throw new IOException();
        }
    }

    @Override
    public void write(Object[] args, File source) throws IOException {
        for (int i = 0; i < args.length; i++) {
            File file = new File(outputDir, String.format("%s.%d", source.getName(), i));
            try (PrintWriter out = new PrintWriter(file)) {
                out.print(format(args[i]));
            }
        }
    }

    private String format(Object arg) {
        if (arg == null) {
            return "null";
        } else if (arg instanceof byte[]) {
            return Arrays.toString((byte[]) arg);
        } else if (arg instanceof short[]) {
            return Arrays.toString((short[]) arg);
        } else if (arg instanceof int[]) {
            return Arrays.toString((int[]) arg);
        } else if (arg instanceof long[]) {
            return Arrays.toString((long[]) arg);
        } else if (arg instanceof char[]) {
            return Arrays.toString((char[]) arg);
        } else if (arg instanceof float[]) {
            return Arrays.toString((float[]) arg);
        } else if (arg instanceof double[]) {
            return Arrays.toString((double[]) arg);
        } else if (arg instanceof boolean[]) {
            return Arrays.toString((boolean[]) arg);
        } else if (arg instanceof Object[]) {
            return Arrays.deepToString((Object[]) arg);
        } else {
            return arg.toString();
        }
    }
}
