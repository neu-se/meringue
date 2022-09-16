package edu.berkeley.cs.jqf;

import edu.berkeley.cs.jqf.replay.ZestReplayer;
import edu.neu.ccs.prl.meringue.*;
import janala.instrument.SnoopInstructionTransformer;
import org.objectweb.asm.ClassVisitor;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ZestFramework implements FuzzFramework {
    private File outputDir;
    private File corpusDir;
    private File failuresDir;
    private JvmLauncher launcher;
    private String argumentsDirPath;

    @Override
    public void initialize(CampaignConfiguration config, Properties frameworkArguments) {
        outputDir = config.getOutputDir();
        corpusDir = new File(outputDir, "corpus");
        failuresDir = new File(outputDir, "failures");
        List<String> javaOptions = new ArrayList<>(config.getJavaOptions());
        File instrumentJar = FileUtil.getClassPathElement(SnoopInstructionTransformer.class);
        File asmJar = FileUtil.getClassPathElement(ClassVisitor.class);
        javaOptions.add(String.format("-Xbootclasspath/a:%s:%s", instrumentJar.getAbsolutePath(),
                                      asmJar.getAbsolutePath()));
        javaOptions.add("-javaagent:" + instrumentJar.getAbsolutePath());
        javaOptions.add("-cp");
        String classPath = config.getTestClassPathJar().getAbsolutePath() + File.pathSeparator +
                FileUtil.getClassPathElement(ZestFramework.class).getAbsolutePath() + File.pathSeparator +
                FileUtil.getClassPathElement(FuzzFramework.class).getAbsolutePath();
        javaOptions.add(classPath);
        String[] arguments = new String[]{
                config.getTestClassName(),
                config.getTestMethodName(),
                outputDir.getAbsolutePath()
        };
        this.argumentsDirPath = System.getProperty("zest.argumentsDir");
        if (argumentsDirPath == null) {
            argumentsDirPath = frameworkArguments.getProperty("argumentsDir");
        }
        launcher = new JvmLauncher.JavaMainLauncher(config.getJavaExec(), ZestForkMain.class.getName(),
                                                    javaOptions.toArray(new String[0]), true, arguments);
    }

    @Override
    public Process startCampaign() throws IOException {
        FileUtil.ensureDirectory(outputDir);
        FileUtil.createOrCleanDirectory(corpusDir);
        FileUtil.createOrCleanDirectory(failuresDir);
        return launcher.launch();
    }

    @Override
    public File[] getCorpusFiles() {
        return corpusDir.listFiles();
    }

    @Override
    public File[] getFailureFiles() {
        return failuresDir.listFiles();
    }

    @Override
    public Class<? extends Replayer> getReplayerClass() {
        return ZestReplayer.class;
    }

    @Override
    public Collection<File> getRequiredClassPathElements() {
        return Stream.of(ZestFramework.class, FuzzFramework.class)
                     .map(FileUtil::getClassPathElement)
                     .collect(Collectors.toList());
    }

    @Override
    public List<String> prepareForAnalysisPhase() throws IOException {
        if (argumentsDirPath != null) {
            FileUtil.createOrCleanDirectory(new File(argumentsDirPath));
            return Collections.singletonList("-Dzest.argumentsDir=" + argumentsDirPath);
        }
        return Collections.emptyList();
    }
}
