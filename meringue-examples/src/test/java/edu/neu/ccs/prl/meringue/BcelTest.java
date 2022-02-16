package edu.neu.ccs.prl.meringue;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.examples.bcel.JavaClassGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.verifier.DirectVerifierFactory;
import org.apache.bcel.verifier.VerificationResult;
import org.apache.bcel.verifier.Verifier;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RunWith(JQF.class)
public class BcelTest {
    public void test(byte[] input) {
        JavaClass clazz;
        try {
            clazz = new ClassParser(new ByteArrayInputStream(input), "Example.class").parse();
        } catch (ClassFormatException | IOException e) {
            return;
        }
        try {
            Repository.addClass(clazz);
            verify(clazz);
        } finally {
            Repository.clearCache();
        }
    }

    @Fuzz
    public void testWithGenerator(@From(JavaClassGenerator.class) JavaClass javaClass) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javaClass.dump(out);
        test(out.toByteArray());
    }

    private static void verify(JavaClass clazz) {
        Verifier verifier = DirectVerifierFactory.getVerifier(clazz.getClassName());
        VerificationResult result = verifier.doPass1();
        if (result.getStatus() != VerificationResult.VERIFIED_OK) {
            return;
        }
        result = verifier.doPass2();
        if (result.getStatus() != VerificationResult.VERIFIED_OK) {
            return;
        }
        for (int i = 0; i < clazz.getMethods().length; i++) {
            result = verifier.doPass3a(i);
            if (result.getStatus() != VerificationResult.VERIFIED_OK) {
                return;
            }
            result = verifier.doPass3b(i);
            if (result.getStatus() != VerificationResult.VERIFIED_OK) {
                return;
            }
        }
    }
}
