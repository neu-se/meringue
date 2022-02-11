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
    private List<String> javaOptions;
    private File failuresDir;
    private CampaignConfiguration config;

    @Override
    public void initialize(CampaignConfiguration config, Properties frameworkOptions) {
        this.outputDir = config.getOutputDir();
        this.config = config;
        this.corpusDir = new File(outputDir, "corpus");
        this.failuresDir = new File(outputDir, "failures");
        this.javaOptions = new ArrayList<>(config.getJavaOptions());
        File instrumentJar = FileUtil.getClassPathElement(SnoopInstructionTransformer.class);
        File asmJar = FileUtil.getClassPathElement(ClassVisitor.class);
        this.javaOptions.add(String.format("-Xbootclasspath/a:%s:%s", instrumentJar.getAbsolutePath(), asmJar.getAbsolutePath()));
        this.javaOptions.add("-javaagent:" + instrumentJar.getAbsolutePath());
    }

    @Override
    public Process startCampaign() throws IOException {
        FileUtil.ensureDirectory(outputDir);
        FileUtil.createOrCleanDirectory(corpusDir);
        FileUtil.createOrCleanDirectory(failuresDir);
        // TODO
        //return ProcessUtil.launchJvm(config.getJavaExec(), config.getClassPathJar(),
        //        javaOptions.toArray(new String[0]),
        //        true,
        //        config.getTestClassName(), config.getTestMethodName(), outputDir.getAbsolutePath(),
        //        String.valueOf(config.getMaxLength())
        //);
        return null;
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
