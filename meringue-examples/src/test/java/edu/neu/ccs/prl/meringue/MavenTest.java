package edu.neu.ccs.prl.meringue;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.examples.common.Dictionary;
import edu.berkeley.cs.jqf.examples.xml.XMLDocumentUtils;
import edu.berkeley.cs.jqf.examples.xml.XmlDocumentGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RunWith(JQF.class)
public class MavenTest {
    public void test(byte[] input) {
        ModelReader reader = new DefaultModelReader();
        try {
            reader.read(new ByteArrayInputStream(input), null);
        } catch (IOException e) {
            //
        }
    }

    @Fuzz
    public void testWithGenerator(@From(XmlDocumentGenerator.class)
                                  @Dictionary("dictionaries/maven-model.dict") Document document) {
        test(XMLDocumentUtils.documentToString(document).getBytes());
    }
}