package edu.neu.ccs.prl.meringue;

import janala.instrument.SnoopInstructionTransformer;
import org.objectweb.asm.ClassVisitor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ZestFramework implements FuzzFramework {
    private File outputDir;
    private File corpusDir;
    private File failuresDir;
    private JvmLauncher launcher;

    @Override
    public void initialize(CampaignConfiguration config, Properties frameworkArguments) {
        outputDir = config.getOutputDir();
        corpusDir = new File(outputDir, "corpus");
        failuresDir = new File(outputDir, "failures");
        List<String> javaOptions = new ArrayList<>(config.getJavaOptions());
        File instrumentJar = FileUtil.getClassPathElement(SnoopInstructionTransformer.class);
        File asmJar = FileUtil.getClassPathElement(ClassVisitor.class);
        javaOptions.add(
                String.format("-Xbootclasspath/a:%s:%s", instrumentJar.getAbsolutePath(), asmJar.getAbsolutePath()));
        javaOptions.add("-javaagent:" + instrumentJar.getAbsolutePath());
        javaOptions.add("-cp");
        String classPath = config.getTestClassPathJar().getAbsolutePath() + File.pathSeparator +
                FileUtil.getClassPathElement(ZestFramework.class).getAbsolutePath() + File.pathSeparator +
                FileUtil.getClassPathElement(FuzzFramework.class).getAbsolutePath();
        javaOptions.add(classPath);
        String[] arguments =
                new String[]{config.getTestClassName(), config.getTestMethodName(), outputDir.getAbsolutePath()};
        launcher = JvmLauncher.fromMain(config.getJavaExec(), ZestForkMain.class.getName(),
                                        javaOptions.toArray(new String[0]), true, arguments,
                                        config.getWorkingDir(),
                                        config.getEnvironment());
    }

    @Override
    public Process startCampaign() throws IOException {
        FileUtil.ensureDirectory(outputDir);
        FileUtil.ensureEmptyDirectory(corpusDir);
        FileUtil.ensureEmptyDirectory(failuresDir);
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
        return Stream.of(ZestFramework.class, FuzzFramework.class).map(FileUtil::getClassPathElement)
                     .collect(Collectors.toList());
    }
}
