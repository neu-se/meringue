package edu.neu.ccs.prl.meringue;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.ProjectHelperAdapter;

import java.io.ByteArrayInputStream;

public class AntTest {
    private static final ProjectHelperAdapter adapter;

    static {
        try {
            adapter = new ProjectHelperAdapter();
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public void parse(byte[] input) {
        try {
            adapter.parse(new Project(), new ByteArrayInputStream(input));
        } catch (BuildException | IllegalAccessException e) {
            //
        }
    }
}
