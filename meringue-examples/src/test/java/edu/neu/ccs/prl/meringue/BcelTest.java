package edu.neu.ccs.prl.meringue;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.verifier.DirectVerifierFactory;
import org.apache.bcel.verifier.VerificationResult;
import org.apache.bcel.verifier.Verifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class BcelTest {
    public void parseAndVerify(byte[] input) {
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
