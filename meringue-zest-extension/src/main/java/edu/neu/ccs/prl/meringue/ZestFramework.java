package edu.neu.ccs.prl.meringue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class ZestFramework implements FuzzFramework {
    private static final String JQF_GROUP_ID = "edu.berkeley.cs.jqf";
    private JvmLauncher launcher;
    private File corpusDirectory;
    private File failuresDirectory;
    private File frameworkJar;
    private ArtifactResolver resolver;
    private File temporaryDirectory;

    @Override
    public void setResolver(ArtifactResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void setTemporaryDirectory(File temporaryDirectory) {
        this.temporaryDirectory = temporaryDirectory;
    }

    @Override
    public void initialize(CampaignConfiguration configuration, Properties frameworkArguments) throws IOException {
        corpusDirectory = new File(configuration.getOutputDirectory(), "corpus");
        failuresDirectory = new File(configuration.getOutputDirectory(), "failures");
        frameworkJar = buildFrameworkJar();
        List<File> bootClasspathElements = resolver.resolve(JQF_GROUP_ID, "jqf-instrument", getJqfVersion(),
                "jar", null, true);
        File agentJar = resolver.resolve(JQF_GROUP_ID, "jqf-instrument", getJqfVersion(), "jar", null, false)
                .iterator()
                .next();
        List<String> javaOptions = new ArrayList<>(configuration.getJavaOptions());
        javaOptions.add("-Xbootclasspath/a:" + resolver.buildClassPath(bootClasspathElements));
        javaOptions.add("-javaagent:" + agentJar.getAbsolutePath());
        javaOptions.add("-cp");
        javaOptions.add(resolver.buildClassPath(Arrays.asList(configuration.getTestClasspathJar(), frameworkJar)));
        if (!hasJanalaConfiguration(javaOptions)) {
            File janalaFile = new File(temporaryDirectory, "janala.conf");
            writeJanalaConfiguration(janalaFile);
            javaOptions.add("-Djanala.conf=" + janalaFile.getAbsolutePath());
        }
        launcher = JvmLauncher.fromMain(
                configuration.getJavaExecutable(),
                getMainClassName(),
                javaOptions.toArray(new String[0]),
                true,
                getArguments(configuration, frameworkArguments),
                configuration.getWorkingDirectory(),
                configuration.getEnvironment()
        );
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

    protected File buildFrameworkJar() throws IOException {
        File result = new File(temporaryDirectory, "jqf-framework.jar");
        List<File> classpathElements =
                new ArrayList<>(resolver.resolve(JQF_GROUP_ID, "jqf-fuzz", getJqfVersion(), "jar", null, true));
        classpathElements.add(FileUtil.getClassPathElement(getReplayerClass()));
        classpathElements.add(FileUtil.getClassPathElement(SystemPropertyUtil.class));
        FileUtil.buildManifestJar(classpathElements, result);
        return result;
    }

    protected String getJqfVersion() {
        return "2.0";
    }

    protected String[] getArguments(CampaignConfiguration configuration, Properties frameworkArguments) {
        return new String[]{
                configuration.getTestClassName(),
                configuration.getTestMethodName(),
                configuration.getOutputDirectory().getAbsolutePath()
        };
    }

    protected String getMainClassName() {
        return ZestForkMain.class.getName();
    }

    protected File[] getInputFiles(File directory) {
        return Arrays.stream(Objects.requireNonNull(directory.listFiles()))
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
                    "edu/berkeley/cs/jqf/",
                    "edu/neu/ccs/prl/meringue/ZestForkMain",
                    "org/junit/"));
            out.println("janala.includes=" + String.join(",",
                    "edu/berkeley/cs/jqf/examples",
                    "java/text",
                    "java/time",
                    "com/sun/imageio"));
        }
    }
}
