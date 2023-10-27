package edu.neu.ccs.prl.meringue;

import com.code_intelligence.jazzer.driver.FuzzTargetHolder;
import com.code_intelligence.jazzer.driver.FuzzedDataProviderImpl;
import com.code_intelligence.jazzer.driver.LifecycleMethodsInvoker;
import com.code_intelligence.jazzer.utils.Log;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public final class FuzzTargetRunner {
    private final MethodHandle fuzzTargetMethod;
    private final LifecycleMethodsInvoker lifecycleMethodsInvoker;
    private final boolean useFuzzedDataProvider;
    private final Object fuzzTargetInstance;

    public FuzzTargetRunner(String testClassName) throws Throwable {
        FuzzTargetHolder.FuzzTarget fuzzTarget = findFuzzTarget(testClassName);
        lifecycleMethodsInvoker = fuzzTarget.lifecycleMethodsInvoker;
        fuzzTarget.method.setAccessible(true);
        fuzzTargetMethod = MethodHandles.lookup().unreflect(fuzzTarget.method);
        useFuzzedDataProvider = fuzzTarget.usesFuzzedDataProvider();
        fuzzTargetInstance = fuzzTarget.newInstance.call();
        lifecycleMethodsInvoker.beforeFirstExecution();
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public Throwable run(byte[] data) {
        try {
            if (useFuzzedDataProvider) {
                try (FuzzedDataProviderImpl provider = FuzzedDataProviderImpl.withJavaData(data)) {
                    runInternal(provider);
                }
            } else {
                runInternal(data);
            }
        } catch (Throwable t) {
            return t;
        }
        return null;
    }

    public void runInternal(Object argument) throws Throwable {
        lifecycleMethodsInvoker.beforeEachExecution();
        if (fuzzTargetInstance == null) {
            fuzzTargetMethod.invoke(argument);
        } else {
            fuzzTargetMethod.invoke(fuzzTargetInstance, argument);
        }
    }

    private void shutdown() {
        try {
            lifecycleMethodsInvoker.afterLastExecution();
        } catch (Throwable t) {
            Log.finding(t);
        }
    }

    private FuzzTargetHolder.FuzzTarget findFuzzTarget(String testClassName) throws ReflectiveOperationException {
        Class<?> clazz = Class.forName("com.code_intelligence.jazzer.driver.FuzzTargetFinder");
        Method m = clazz.getDeclaredMethod("findFuzzTarget", String.class);
        m.setAccessible(true);
        return (FuzzTargetHolder.FuzzTarget) m.invoke(null, testClassName);
    }
}
