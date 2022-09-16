package edu.berkeley.cs.jqf.replay;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;

public class FileArgumentsWriterTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void writeNullArgument() throws IOException {
        Object argument = null;
        String[] s = writeRead(new Object[]{argument});
        Assert.assertArrayEquals(new String[]{"null"}, s);
    }

    @Test
    public void writeStringArgument() throws IOException {
        Object argument = "hello";
        writeRead(new Object[]{argument});
        String[] s = writeRead(new Object[]{argument});
        Assert.assertArrayEquals(new String[]{"hello"}, s);
    }

    @Test
    public void writeStringArrayArgument() throws IOException {
        Object argument = new String[]{"hello", "world"};
        writeRead(new Object[]{argument});
        String[] s = writeRead(new Object[]{argument});
        Assert.assertArrayEquals(new String[]{"[hello, world]"}, s);
    }

    @Test
    public void writeStringArrayNullElementArgument() throws IOException {
        Object argument = new String[]{"hello", null};
        writeRead(new Object[]{argument});
        String[] s = writeRead(new Object[]{argument});
        Assert.assertArrayEquals(new String[]{"[hello, null]"}, s);
    }

    @Test
    public void writeIntArrayArgument() throws IOException {
        Object argument = new int[]{8, 99, 77};
        String[] s = writeRead(new Object[]{argument});
        Assert.assertArrayEquals(new String[]{"[8, 99, 77]"}, s);
    }

    @Test
    public void writeIntDoubleArrayArgument() throws IOException {
        Object argument = new int[][]{{8}, {99, 7}, {}};
        String[] s = writeRead(new Object[]{argument});
        Assert.assertArrayEquals(new String[]{"[[8], [99, 7], []]"}, s);
    }

    @Test(timeout = 500)
    public void writeObjectArraySelfReference() throws IOException {
        Object[] argument = new Object[]{"hello", "world"};
        argument[1] = argument;
        String[] s = writeRead(new Object[]{argument});
        Assert.assertArrayEquals(new String[]{"[hello, [...]]"}, s);
    }

    @Test
    public void writeMultiArrayArguments() throws IOException {
        int[] argument0 = new int[]{8, 99, 77};
        int[] argument1 = new int[]{7, -2};
        String[] s = writeRead(new Object[]{argument0, argument1});
        Assert.assertArrayEquals(new String[]{"[8, 99, 77]", "[7, -2]"}, s);
    }

    private String[] writeRead(Object[] args) throws IOException {
        File source = folder.newFile();
        File outputDir = folder.newFolder();
        FileArgumentsWriter writer = new FileArgumentsWriter(outputDir);
        writer.write(args, source);
        return Arrays.stream(Objects.requireNonNull(outputDir.listFiles())).sorted()
                     .map(FileArgumentsWriterTest::readString).toArray(String[]::new);
    }

    private static String readString(File file) {
        try {
            byte[] encoded = Files.readAllBytes(file.toPath());
            return new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}