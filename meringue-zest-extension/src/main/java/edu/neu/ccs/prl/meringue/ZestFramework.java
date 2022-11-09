package edu.neu.ccs.prl.meringue;

import janala.instrument.SnoopInstructionTransformer;
import org.objectweb.asm.ClassVisitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class ZestFramework implements JarFuzzFramework {
    private JvmLauncher launcher;
    private File frameworkJar;
    private File corpusDirectory;
    private File failuresDirectory;

    @Override
    public void initialize(CampaignConfiguration configuration, Properties frameworkArguments) throws IOException {
        File outputDirectory = configuration.getOutputDirectory();
        FileUtil.ensureDirectory(outputDirectory);
        corpusDirectory = new File(outputDirectory, "corpus");
        failuresDirectory = new File(outputDirectory, "failures");
        List<String> javaOptions = new ArrayList<>(configuration.getJavaOptions());
        File instrumentJar = FileUtil.getClassPathElement(SnoopInstructionTransformer.class);
        File asmJar = FileUtil.getClassPathElement(ClassVisitor.class);
        javaOptions.add(
                String.format("-Xbootclasspath/a:%s:%s", instrumentJar.getAbsolutePath(), asmJar.getAbsolutePath()));
        javaOptions.add("-javaagent:" + instrumentJar.getAbsolutePath());
        javaOptions.add("-cp");
        String classPath = configuration.getTestClasspathJar().getAbsolutePath() + File.pathSeparator +
                frameworkJar.getAbsolutePath();
        javaOptions.add(classPath);
        if (!hasJanalaConfiguration(javaOptions)) {
            File janalaFile = new File(outputDirectory, "janala.conf");
            writeJanalaConfiguration(janalaFile);
            javaOptions.add("-Djanala.conf=" + janalaFile.getAbsolutePath());
        }
        String[] arguments =
                new String[]{configuration.getTestClassName(), configuration.getTestMethodName(),
                        outputDirectory.getAbsolutePath()};
        launcher = JvmLauncher.fromMain(configuration.getJavaExecutable(), getMainClassName(),
                                        javaOptions.toArray(new String[0]), true, arguments,
                                        configuration.getWorkingDirectory(),
                                        configuration.getEnvironment());
    }

    @Override
    public Process startCampaign() throws IOException {
        FileUtil.ensureEmptyDirectory(corpusDirectory);
        FileUtil.ensureEmptyDirectory(failuresDirectory);
        return launcher.launch();
    }

    @Override
    public File[] getCorpusFiles() {
        return getInputFiles(corpusDirectory);
    }

    @Override
    public File[] getFailureFiles() {
        return getInputFiles(failuresDirectory);
    }

    @Override
    public Class<? extends Replayer> getReplayerClass() {
        return ZestReplayer.class;
    }

    @Override
    public Collection<File> getRequiredClassPathElements() {
        return Collections.singleton(frameworkJar);
    }

    @Override
    public boolean canRestartCampaign() {
        return false;
    }

    @Override
    public Process restartCampaign() {
        throw new UnsupportedOperationException();
    }

    public String getMainClassName() {
        return ZestForkMain.class.getName();
    }

    @Override
    public String getCoordinate() {
        return "edu.neu.ccs.prl.meringue:meringue-zest-extension";
    }

    @Override
    public void setFrameworkJar(File frameworkJar) {
        this.frameworkJar = frameworkJar;
    }

    private File[] getInputFiles(File corpusDirectory) {
        return Arrays.stream(Objects.requireNonNull(corpusDirectory.listFiles()))
                     .filter(f -> f.getName().startsWith("id_"))
                     .toArray(File[]::new);
    }

    private static boolean hasJanalaConfiguration(List<String> javaOptions) {
        for (String javaOption : javaOptions) {
            if (javaOption.startsWith("-Djanala.conf=")) {
                return true;
            }
        }
        return false;
    }

    private static void writeJanalaConfiguration(File file) throws FileNotFoundException {
        try (PrintWriter out = new PrintWriter(file)) {
            out.println("janala.excludes=" + String.join(",",
                                                         "java/",
                                                         "com/sun/proxy/",
                                                         "edu/berkeley/cs/jqf/"));
            out.println("janala.includes=" + String.join(",",
                                                         "edu/berkeley/cs/jqf/examples",
                                                         "java/text",
                                                         "java/time",
                                                         "com/sun/imageio"));
        }
    }
}
