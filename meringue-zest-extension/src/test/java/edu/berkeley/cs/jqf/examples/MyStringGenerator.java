package edu.berkeley.cs.jqf.examples;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class MyStringGenerator extends Generator<String> {
    public MyStringGenerator() {
        super(String.class);
    }

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        int size = random.nextInt(100);
        char[] c = new char[size];
        for (int i = 0; i < c.length; i++) {
            c[i] = random.nextChar(Character.MIN_VALUE, Character.MAX_VALUE);
        }
        return new String(c);
    }
}