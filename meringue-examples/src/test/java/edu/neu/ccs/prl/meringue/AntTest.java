package edu.neu.ccs.prl.meringue;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.examples.common.Dictionary;
import edu.berkeley.cs.jqf.examples.xml.XMLDocumentUtils;
import edu.berkeley.cs.jqf.examples.xml.XmlDocumentGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.ProjectHelperAdapter;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;

@RunWith(JQF.class)
public class AntTest {
    private static final ProjectHelperAdapter adapter;

    static {
        try {
            adapter = new ProjectHelperAdapter();
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public void test(byte[] input) {
        try {
            adapter.parse(new Project(), new ByteArrayInputStream(input));
        } catch (BuildException | IllegalAccessException e) {
            //
        }
    }

    @Fuzz
    public void testWithGenerator(@From(XmlDocumentGenerator.class)
                                  @Dictionary("dictionaries/ant-project.dict") Document document) {
        test(XMLDocumentUtils.documentToString(document).getBytes());
    }
}
