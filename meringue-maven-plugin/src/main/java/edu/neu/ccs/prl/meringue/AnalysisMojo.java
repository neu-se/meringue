package edu.neu.ccs.prl.meringue;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.surefire.SurefireHelper;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.jacoco.agent.rt.internal_3570298.PreMain;
import org.jacoco.core.analysis.Analyzer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maven plugin that analyzes the results of a fuzzing campaign.
 */
@Mojo(name = "analyze", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class AnalysisMojo extends AbstractMeringueMojo {
    /**
     * List of JARs, directories, and files to be included in the reports.
     */
    @Parameter
    List<File> includedClassPathElements = new LinkedList<>();
    /**
     * List of class files to include in reports. May use wildcard
     * characters (* and ?). By default, all files are included.
     */
    @Parameter
    private List<String> inclusions = new LinkedList<>();
    /**
     * List of class files to exclude from reports. May use wildcard
     * characters (* and ?). By default, no files are excluded.
     */
    @Parameter
    private List<String> exclusions = new LinkedList<>();
    /**
     * Maximum number of frames to include in stack traces taken for failures.
     * By default, a maximum of {@code 5} frames are included.
     */
    @Parameter(property = "meringue.maxTraceSize", defaultValue = "5")
    private int maxTraceSize;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Running analysis for: " + getTestDescription());
        try {
            CampaignConfiguration config = createConfiguration();
            FuzzFramework framework = createFramework(config);
            List<File> inputFiles = collectInputFiles(framework);
            if (inputFiles.isEmpty()) {
                getLog().info("No input files were found for analysis");
                return;
            }
            CoverageFilter filter = new CoverageFilter(inclusions, exclusions, includedClassPathElements);
            JvmLauncher launcher = createLauncher(config, framework, filter);
            // Create a server socket bound to an automatically allocated port
            try (ServerSocket server = new ServerSocket(0)) {
                // Launch the analysis JVM
                Process process = launcher.launch(new String[]{String.valueOf(server.getLocalPort())});
                try (ForkConnection connection = new ForkConnection(server.accept())) {
                    analyze(config, framework, inputFiles, connection);
                }
                if (ProcessUtil.waitFor(process) != 0) {
                    throw new IOException("Error occurred in forked analysis process");
                }
            }
        } catch (IOException | InterruptedException | ReflectiveOperationException e) {
            throw new MojoExecutionException("Failed to analyze fuzzing campaign", e);
        }
    }

    private JvmLauncher createLauncher(CampaignConfiguration config, FuzzFramework framework, CoverageFilter filter)
            throws MojoExecutionException {
        List<String> options = new LinkedList<>(config.getJavaOptions());
        if (config.isDebug()) {
            options.add(JvmLauncher.DEBUG_OPT + "5005");
        }
        String classPath = Stream.of(createAnalysisManifestJar(config), config.getTestClassPathJar(),
                        createFrameworkClassPathJar(framework))
                .map(File::getAbsolutePath)
                .map(SurefireHelper::escapeToPlatformPath)
                .collect(Collectors.joining(File.pathSeparator));
        options.add("-cp");
        options.add(classPath);
        options.add(filter.getJacocoOption());
        return new JvmLauncher.JavaMainLauncher(
                config.getJavaExec(),
                AnalysisForkMain.class.getName(),
                options.toArray(new String[0]),
                config.isDebug(),
                new String[0]
        );
    }

    public void analyze(CampaignConfiguration config, FuzzFramework framework, List<File> inputFiles,
                        ForkConnection connection)
            throws IOException, InterruptedException, ReflectiveOperationException {
        long firstTimestamp = inputFiles.isEmpty() ? 0 : inputFiles.get(0).lastModified();
        connection.send(config.getTestClassName());
        connection.send(config.getTestMethodName());
        connection.send(framework.getReplayerClass().getName());
        connection.send(maxTraceSize);
        connection.send(inputFiles.toArray(new File[0]));
        int i = 0;
        for (File inputFile : inputFiles) {
            long time = inputFile.lastModified() - firstTimestamp;
            byte[] execData = connection.receive(byte[].class);
            if (connection.receive(Boolean.class)) {
                StackTraceElement[] trace = connection.receive(StackTraceElement[].class);
            }
            // TODO write results to report
            if ((i + 1) % 100 == 0) {
                System.out.printf("Analyzed %d/%d input files%n", i + 1, inputFiles.size());
            }
            i++;
        }
        // Send the shutdown signal
        connection.send(null);
    }

    private static File createAnalysisManifestJar(CampaignConfiguration config)
            throws MojoExecutionException {
        try {
            File jar = new File(config.getOutputDir(), "analysis.jar");
            FileUtil.buildManifestJar(Arrays.asList(
                    FileUtil.getClassPathElement(AnalysisForkMain.class),
                    FileUtil.getClassPathElement(PreMain.class),
                    FileUtil.getClassPathElement(Analyzer.class)), jar);
            return jar;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create analysis manifest JAR", e);
        }
    }

    private static List<File> collectInputFiles(FuzzFramework framework) throws IOException {
        List<File> files = new LinkedList<>(Arrays.asList(framework.getCorpusFiles()));
        files.addAll(Arrays.asList(framework.getFailureFiles()));
        files.sort(Comparator.comparingLong(File::lastModified));
        return files;
    }
}