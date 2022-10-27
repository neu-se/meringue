package edu.neu.ccs.prl.meringue;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.dependency.utils.translators.ArtifactTranslator;
import org.apache.maven.plugins.dependency.utils.translators.ClassifierTypeTranslator;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ArtifactSourceResolver {
    private final Log log;
    private final MavenSession session;
    private final ArtifactResolver resolver;
    private final ArtifactHandlerManager handlerManager;

    public ArtifactSourceResolver(Log log, MavenSession session, ArtifactResolver resolver,
                                  ArtifactHandlerManager handlerManager) {
        if (log == null || session == null || resolver == null || handlerManager == null) {
            throw new NullPointerException();
        }
        this.log = log;
        this.session = session;
        this.resolver = resolver;
        this.handlerManager = handlerManager;
    }

    public Set<File> getSources(MavenProject project, Set<Artifact> artifacts) {
        ArtifactTranslator translator = new ClassifierTypeTranslator(handlerManager, "sources", "");
        Collection<ArtifactCoordinate> coordinates = translator.translate(artifacts, log);
        return resolve(project, new LinkedHashSet<>(coordinates)).stream()
                                                                 .map(Artifact::getFile)
                                                                 .filter(f -> f != null && f.exists())
                                                                 .collect(Collectors.toSet());
    }

    private Set<Artifact> resolve(MavenProject project, Set<ArtifactCoordinate> coordinates) {
        ProjectBuildingRequest request = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        request.setRemoteRepositories(project.getRemoteArtifactRepositories());
        Set<Artifact> result = new LinkedHashSet<>();
        for (ArtifactCoordinate coordinate : coordinates) {
            try {
                result.add(resolver.resolveArtifact(request, coordinate).getArtifact());
            } catch (ArtifactResolverException ex) {
                log.debug("Error resolving: " + coordinate);
            }
        }
        return result;
    }
}
