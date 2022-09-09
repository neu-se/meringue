package edu.berkeley.cs.jqf;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.internal.ParameterTypeContext;
import com.pholser.junit.quickcheck.internal.generator.GeneratorRepository;
import com.pholser.junit.quickcheck.internal.generator.ServiceLoaderGeneratorSource;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.guidance.TimeoutException;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FastSourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.NonTrackingGenerationStatus;
import org.junit.AssumptionViolatedException;

import java.io.EOFException;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArgumentsGenerator {
    private final List<Generator<?>> generators;

    public ArgumentsGenerator(List<Field> fields, long seed) {
        this(fields.stream().map(f -> ParameterTypeContext.forField(f).annotate(f)), seed);
    }

    public ArgumentsGenerator(Executable executable, long seed) {
        this(Arrays.stream(executable.getParameters()).map(p -> ParameterTypeContext.forParameter(p).annotate(p)), seed);
    }

    private ArgumentsGenerator(Stream<ParameterTypeContext> contexts, long seed) {
        SourceOfRandomness randomness = new SourceOfRandomness(new Random(seed));
        GeneratorRepository repository =
                new GeneratorRepository(randomness).register(new ServiceLoaderGeneratorSource());
        this.generators = Collections.unmodifiableList(
                contexts.map(x -> createGenerator(repository, x)).collect(Collectors.toList()));
    }

    public List<Generator<?>> getGenerators() {
        return generators;
    }

    public Object[] generate(Guidance guidance) {
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

    private static Generator<?> createGenerator(GeneratorRepository repository, ParameterTypeContext context) {
        Generator<?> generator = repository.generatorFor(context);
        generator.provide(repository);
        generator.configure(context.annotatedType());
        if (context.topLevel()) {
            generator.configure(context.annotatedElement());
        }
        return generator;
    }
}
