package edu.neu.ccs.prl.meringue;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.SourceFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.LogManager;

public class ClosureTest {
    static {
        // Disable logging by Closure passes
        LogManager.getLogManager().reset();
    }

    public void compile(byte[] input) throws IOException {
        Compiler compiler = new Compiler(new PrintStream(new ByteArrayOutputStream(), false));
        SourceFile extern = SourceFile.fromCode("externs", "");
        CompilerOptions options = new CompilerOptions();
        compiler.disableThreads();
        options.setPrintConfig(false);
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
        compiler.compile(extern, SourceFile.fromInputStream("input", new ByteArrayInputStream(input),
                StandardCharsets.UTF_8), options);
    }
}
