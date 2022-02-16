package edu.neu.ccs.prl.meringue;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class JazzerTarget {
    private final Constructor<?> testConstructor;
    private final Method testMethod;
    private final boolean takesRawBytes;

    public JazzerTarget(String testClassName, String testMethodName, ClassLoader classLoader) {
        if (testClassName == null || testMethodName == null) {
            throw new NullPointerException();
        }
        Class<?> testClass = findTestClass(testClassName, classLoader);
        this.testConstructor = getTestConstructor(testClass);
        this.testMethod = findTestMethod(testClass, testMethodName);
        this.takesRawBytes = testMethod.getParameterTypes()[0].equals(byte[].class);
    }

    public Constructor<?> getTestConstructor() {
        return testConstructor;
    }

    public Method getTestMethod() {
        return testMethod;
    }

    public void execute(FuzzedDataProvider input) throws InvocationTargetException {
        Object testInstance;
        try {
            testInstance = testConstructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to create test instance", e);
        }
        try {
            Object[] args = new Object[]{takesRawBytes ? input.consumeRemainingAsBytes() : input};
            testMethod.invoke(testInstance, args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to call test method", e);
        }
    }

    private static Constructor<?> getTestConstructor(Class<?> testClass) {
        try {
            Constructor<?> constructor = testClass.getConstructor();
            if (!Modifier.isPublic(constructor.getModifiers())) {
                throw new IllegalArgumentException("Test class should have a public, zero-argument constructor");
            }
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Test class should have a public, zero-argument constructor");
        }
    }

    private static Class<?> findTestClass(String className, ClassLoader classLoader) {
        try {
            Class<?> testClass = Class.forName(className, true, classLoader);
            if (!isConcrete(testClass)) {
                throw new IllegalArgumentException("Test class must be concrete");
            }
            return testClass;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find test class:" + className);
        }
    }

    private static boolean isConcrete(Class<?> clazz) {
        return !Modifier.isAbstract(clazz.getModifiers()) && !clazz.isInterface();
    }

    private static Method findTestMethod(Class<?> testClass, String methodName, Class<?>... parameterTypes) {
        Method testMethod = null;
        for (Method m : testClass.getMethods()) {
            if (m.getName().equals(methodName) && isValidTestMethod(m)) {
                if (testMethod == null) {
                    testMethod = m;
                } else {
                    throw new IllegalArgumentException("Found multiple public, non-static, void methods in class "
                            + testClass + " with name" + methodName);
                }
            }
        }
        if (testMethod == null) {
            throw new IllegalArgumentException("Could not find public, non-static, void method in class " + testClass +
                    " with name" + methodName);
        }
        testMethod.setAccessible(true);
        return testMethod;
    }

    private static boolean isValidTestMethod(Method m) {
        return !m.isBridge()
                && !m.isSynthetic()
                && !Modifier.isStatic(m.getModifiers())
                && Modifier.isPublic(m.getModifiers())
                && m.getReturnType() == Void.TYPE
                && hasValidParameters(m);
    }

    private static boolean hasValidParameters(Method m) {
        if (m.getParameterTypes().length != 1) {
            return false;
        }
        Class<?> param = m.getParameterTypes()[0];
        return param.equals(byte[].class) || param.equals(FuzzedDataProvider.class);
    }
}
