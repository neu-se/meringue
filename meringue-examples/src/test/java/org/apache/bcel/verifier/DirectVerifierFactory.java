package org.apache.bcel.verifier;

public class DirectVerifierFactory {
    public static Verifier getVerifier(String className) {
        return new Verifier(className);
    }
}
