package edu.neu.ccs.prl.meringue;

import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class MavenTest {

    public void readModel(byte[] input) {
        ModelReader reader = new DefaultModelReader();
        try {
            reader.read(new ByteArrayInputStream(input), null);
        } catch (IOException e) {
            //
        }
    }
}