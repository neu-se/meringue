package edu.berkeley.cs.jqf.replay;

import java.io.File;
import java.io.IOException;

public interface ArgumentsWriter {
    void write(Object[] args, File source) throws IOException;
}
