package edu.berkeley.cs.jqf;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.internal.ParameterTypeContext;
import com.pholser.junit.quickcheck.internal.generator.GeneratorRepository;
import com.pholser.junit.quickcheck.internal.generator.ServiceLoaderGeneratorSource;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.*;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FastSourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.NonTrackingGenerationStatus;
import edu.berkeley.cs.jqf.instrument.InstrumentationException;
import org.junit.AssumptionViolatedException;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized;
import org.junit.runners.model.*;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.TestWithParameters;

import java.io.EOFException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static edu.berkeley.cs.jqf.fuzz.guidance.Result.*;

class ParameterizedZestStatement extends Statement {
    private final FrameworkMethod method;
    private final TestClass clazz;
    private final List<Generator<?>> generators;
    private final List<Throwable> failures = new LinkedList<>();
    private final Set<List<StackTraceElement>> traces = new HashSet<>();
    private final Guidance guidance;

    public ParameterizedZestStatement(TestClass clazz, FrameworkMethod method, Guidance guidance) {
        if (method == null | clazz == null || guidance == null) {
            throw new NullPointerException();
        }
        this.method = method;
        this.clazz = clazz;
        this.guidance = guidance;
        this.generators = createGenerators(clazz);
    }

    public void evaluate() throws MultipleFailureException {
        while (guidance.hasInput()) {
            Result result = SUCCESS;
            Throwable error = null;
            try {
                Object[] arguments = generateArguments();
                guidance.observeGeneratedArgs(arguments);
                ZestNotifier notifier = new ZestNotifier();
                new FilteringParameterizedRunner(arguments).run(notifier);
                if (notifier.failure != null) {
                    throw notifier.failure;
                }
            } catch (InstrumentationException e) {
                throw new GuidanceException(e);
            } catch (GuidanceException e) {
                throw e;
            } catch (AssumptionViolatedException e) {
                result = INVALID;
                error = e;
            } catch (TimeoutException e) {
                result = TIMEOUT;
                error = e;
            } catch (Throwable e) {
                result = FAILURE;
                error = e;
                if (traces.add(Arrays.asList(e.getStackTrace()))) {
                    failures.add(e);
                }
            }
            guidance.handleResult(result, error);
        }
        if (!failures.isEmpty()) {
            throw new MultipleFailureException(failures);
        }
    }

    private Object[] generateArguments() {
        try {
            StreamBackedRandom randomFile = new StreamBackedRandom(guidance.getInput(), Long.BYTES);
            SourceOfRandomness random = new FastSourceOfRandomness(randomFile);
            GenerationStatus genStatus = new NonTrackingGenerationStatus(random);
            return generators.stream().map(g -> g.generate(random, genStatus)).toArray();
        } catch (IllegalStateException e) {
            if (e.getCause() instanceof EOFException) {
                throw new AssumptionViolatedException("Fuzzed input size exceeded", e.getCause());
            } else {
                throw e;
            }
        } catch (AssumptionViolatedException | TimeoutException | GuidanceException e) {
            throw e;
        } catch (Throwable e) {
            // Wrap and rethrow
            throw new GuidanceException(e);
        }
    }

    private static Generator<?> produceGenerator(GeneratorRepository generatorRepository,
                                                 ParameterTypeContext parameter) {
        Generator<?> generator = generatorRepository.generatorFor(parameter);
        generator.provide(generatorRepository);
        generator.configure(parameter.annotatedType());
        if (parameter.topLevel()) {
            generator.configure(parameter.annotatedElement());
        }
        return generator;
    }

    static List<Generator<?>> createGenerators(TestClass clazz) {
        SourceOfRandomness randomness = new SourceOfRandomness(new Random(42));
        GeneratorRepository generatorRepository =
                new GeneratorRepository(randomness).register(new ServiceLoaderGeneratorSource());
        if (clazz.getAnnotatedFields(Parameterized.Parameter.class).isEmpty()) {
            Constructor<?> constructor = clazz.getOnlyConstructor();
            return Arrays.stream(constructor.getParameters()).map(ParameterTypeContext::forParameter)
                         .map(x -> produceGenerator(generatorRepository, x)).collect(Collectors.toList());
        } else {
            return getInjectedFields(clazz).stream().map(ParameterTypeContext::forField)
                                           .map(x -> produceGenerator(generatorRepository, x))
                                           .collect(Collectors.toList());
        }
    }

    static List<Field> getInjectedFields(TestClass clazz) {
        return clazz.getAnnotatedFields(Parameterized.Parameter.class).stream().map(FrameworkField::getField)
                    .sorted(Comparator.comparing(f -> f.getAnnotation(Parameterized.Parameter.class).value()))
                    .collect(Collectors.toList());
    }

    private static final class ZestNotifier extends RunNotifier {
        Throwable failure;

        @Override
        public void fireTestFailure(Failure failure) {
            this.failure = failure.getException();
        }

        @Override
        public void fireTestAssumptionFailed(Failure failure) {
            this.failure = failure.getException();
        }
    }

    private final class FilteringParameterizedRunner extends BlockJUnit4ClassRunnerWithParameters {
        public FilteringParameterizedRunner(Object[] arguments) throws InitializationError {
            super(new TestWithParameters("[0]", clazz, Arrays.asList(arguments)));
        }

        @Override
        protected void runChild(FrameworkMethod method, RunNotifier notifier) {
            if (method.equals(ParameterizedZestStatement.this.method)) {
                super.runChild(method, notifier);
            }
        }
    }
}
