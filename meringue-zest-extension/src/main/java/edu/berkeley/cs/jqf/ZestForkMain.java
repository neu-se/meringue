package edu.berkeley.cs.jqf;

import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runners.Parameterized;
import org.junit.runners.model.MultipleFailureException;
import junitparams.JUnitParamsRunner;

import javassist.NotFoundException;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;

import java.io.File;

public final class ZestForkMain {
    private ZestForkMain() {
        throw new AssertionError(
                getClass().getSimpleName() + " is a static utility class and should not be instantiated");
    }

    public static void main(String[] args) throws Throwable {
        String testClassName = args[0];
        String testMethodName = args[1];
        File outputDir = new File(args[2]);
        Guidance guidance = new ZestGuidance(testClassName + "#" + testMethodName, null, outputDir);
        ClassLoader testClassLoader = ZestForkMain.class.getClassLoader();
        Class<?> testClass = java.lang.Class.forName(testClassName, true, testClassLoader);
        run(testClass, testMethodName, guidance);
    }

    static void run(Class<?> testClass, String testMethodName, Guidance guidance) throws MultipleFailureException {
        Class<? extends Runner> runnerClass = getRunnerClass(testClass);
        if (runnerClass.equals(JQF.class)) {
            GuidedFuzzing.run(testClass, testMethodName, guidance, System.out);
        } else if (runnerClass.equals(Parameterized.class)) {
            ParameterizedZestRunner.run(testClass, testMethodName, guidance);
        } else if (runnerClass.equals(JUnitParamsRunner.class)) {
            try {
                //testClass = addJQFAnnotationToClass(testClass);
                addJQFAnnotationToClass(testClass);
            } catch (Exception e) {
                System.out.println(e.toString());
                throw new IllegalArgumentException("Unknown test class type:" + testClass);
            }
            RunWith annotation = testClass.getAnnotation(RunWith.class);
            System.out.println(annotation.value().getSimpleName());
            GuidedFuzzing.run(testClass, testMethodName, guidance, System.out);
        } else {
            throw new IllegalArgumentException("Unknown test class runner type:" + runnerClass);
        }
    }

    private static Class<? extends Runner> getRunnerClass(Class<?> testClass) {
        if (!testClass.isAnnotationPresent(RunWith.class)) {
            throw new IllegalArgumentException("Test class must be annotated with RunWith: " + testClass);
        }
        RunWith annotation = testClass.getAnnotation(RunWith.class);
        return annotation.value();
    }
    
    private static void addJQFAnnotationToClass (Class<?> testClass) throws NotFoundException,CannotCompileException {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.makeClass(testClass.getName());

        ClassFile classFile = ctClass.getClassFile();
        ConstPool constpool = classFile.getConstPool();

        AnnotationsAttribute annotationsAttribute = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
        Annotation annotation = new Annotation(RunWith.class.getName(), constpool);
        annotation.addMemberValue("value()", new ClassMemberValue(JQF.class.getName(), classFile.getConstPool()));
        annotationsAttribute.setAnnotation(annotation);

        ctClass.getClassFile().addAttribute(annotationsAttribute);
        //Class<?> ret = ctClass.toClass();
        //System.out.println("GOODDDD");

        //return ret;
    }
}
