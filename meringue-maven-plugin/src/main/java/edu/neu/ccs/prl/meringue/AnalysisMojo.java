package edu.neu.ccs.prl.meringue;

import edu.neu.ccs.prl.meringue.internal.AnalysisForkMain;
import edu.neu.ccs.prl.meringue.internal.ForkConnection;
import edu.neu.ccs.prl.meringue.internal.StackTraceCleaner;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.jacoco.agent.rt.internal_3570298.PreMain;
import org.jacoco.core.analysis.Analyzer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;

/**
 * Maven plugin that analyzes the results of a fuzzing campaign.
 */
@Mojo(name = "analyze", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class AnalysisMojo extends AbstractMeringueMojo {
    /**
     * Prefix of packages within meringue that should be excluded from coverage reports.
     */
    private static final String INTERNAL_PACKAGE_PREFIX = "edu/neu/ccs/prl/meringue/internal";
    /**
     * List of JARs, directories, and files to be included in the campaign report.
     */
    @Parameter
    List<File> includedClassPathElements = new LinkedList<>();
    /**
     * A list of class files to include in coverage reports. May use wildcard
     * characters (* and ?). By default, all files are included.
     */
    @Parameter
    private List<String> inclusions = new LinkedList<>();
    /**
     * A list of class files to exclude from coverage reports. May use wildcard
     * characters (* and ?). By default, no files are excluded.
     */
    @Parameter
    private List<String> exclusions = new LinkedList<>();

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Starting analysis for campaign: " + getTestDescription());
        exclusions.add(INTERNAL_PACKAGE_PREFIX + "/*");
        try {
            CampaignConfiguration config = createConfiguration(AnalysisForkMain.class,
                    PluginUtil.getClassPathElement(PreMain.class), PluginUtil.getClassPathElement(Analyzer.class));
            CoverageFilter filter = new CoverageFilter(inclusions, exclusions, includedClassPathElements);
            FuzzFramework framework = createFramework();
            List<File> inputFiles = collectInputFiles(framework, config, getOptions());
            if (inputFiles.isEmpty()) {
                getLog().info("No input files were found for analysis");
            } else {
                analyze(config, framework, filter, inputFiles, new StackTraceCleaner(5));
            }
        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            throw new MojoExecutionException("Failed to analyze fuzzing campaign", e);
        }
    }

    public void analyze(CampaignConfiguration config, FuzzFramework framework, CoverageFilter filter,
                        List<File> inputFiles, StackTraceCleaner cleaner)
            throws IOException, InterruptedException, ClassNotFoundException {
        // Create a server socket bound to an automatically allocated port
        try (ServerSocket server = new ServerSocket(0)) {
            // Launch the analysis JVM
            // TODO use class path JVM option to add class path elements for the Framework
            Process process = JvmLauncher.launchJvm(config.getJavaExec(), config.getTestClassPathJar(),
                    new String[]{filter.getJacocoOption()},
                    true, String.valueOf(server.getLocalPort())
            );
            long firstTimestamp = inputFiles.isEmpty() ? 0 : inputFiles.get(0).lastModified();
            try (ForkConnection connection = new ForkConnection(server.accept())) {
                connection.send(config.getTestClassName());
                connection.send(config.getTestMethodName());
                connection.send(framework.getReplayerClass(config, getOptions()).getName());
                connection.send(inputFiles);
                connection.send(cleaner);
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
            if (ProcessUtil.waitFor(process) != 0) {
                throw new IOException("Error occurred in forked analysis process");
            }
        }
    }

    private static List<File> collectInputFiles(FuzzFramework framework, CampaignConfiguration config,
                                                Properties options) {
        List<File> files = new LinkedList<>(Arrays.asList(framework.getCorpusFiles(config, options)));
        files.addAll(Arrays.asList(framework.getFailureFiles(config, options)));
        files.sort(Comparator.comparingLong(File::lastModified));
        return files;
    }
}