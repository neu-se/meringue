package edu.neu.ccs.prl.meringue;

import janala.instrument.SnoopInstructionTransformer;
import org.objectweb.asm.ClassVisitor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
        javaOptions.add(String.format("-Xbootclasspath/a:%s:%s", instrumentJar.getAbsolutePath(),
                asmJar.getAbsolutePath()));
        javaOptions.add("-javaagent:" + instrumentJar.getAbsolutePath());
        javaOptions.add("-cp");
        // TODO escape path
        // TODO zest class path stuff
        javaOptions.add(config.getTestClassPathJar().getAbsolutePath());
        String[] arguments = new String[]{
                config.getTestClassName(),
                config.getTestMethodName(),
                outputDir.getAbsolutePath()
        };
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
    public File[] getFrameworkClassPathElements() {
        return new File[0];
    }
}
