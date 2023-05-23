package edu.neu.ccs.prl.meringue;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.repository.RepositorySystem;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public final class DependencyResolver implements ArtifactResolver {
    private static final ArtifactFilter RUNTIME_FILTER = new ArtifactFilter() {
        private final Set<String> SCOPES = new HashSet<>(
                Arrays.asList(Artifact.SCOPE_COMPILE, Artifact.SCOPE_COMPILE_PLUS_RUNTIME, Artifact.SCOPE_RUNTIME,
                        null));

        @Override
        public boolean include(Artifact artifact) {
            return !artifact.isOptional() && SCOPES.contains(artifact.getScope());
        }
    };
    private final RepositorySystem repositorySystem;
    private final ResolutionErrorHandler errorHandler;
    private final ArtifactHandlerManager manager;
    private final RepositoryRequest baseRequest;

    public DependencyResolver(CampaignValues values) throws MojoExecutionException {
        this(values.getRepositorySystem(),
                values.getLocalRepository(),
                values.getProject().getPluginArtifactRepositories(),
                values.getErrorHandler(),
                values.getSession().isOffline(),
                values.getArtifactHandlerManager());
    }

    public DependencyResolver(RepositorySystem repositorySystem, ArtifactRepository localRepository,
                              List<ArtifactRepository> remoteRepositories,
                              ResolutionErrorHandler errorHandler, boolean offline, ArtifactHandlerManager manager) {
        if (repositorySystem == null || errorHandler == null || manager == null) {
            throw new NullPointerException();
        }
        this.repositorySystem = repositorySystem;
        this.errorHandler = errorHandler;
        this.manager = manager;
        this.baseRequest = new ArtifactResolutionRequest()
                .setOffline(offline)
                .setLocalRepository(localRepository)
                .setRemoteRepositories(remoteRepositories);

    }

    @Override
    public List<File> resolve(String groupId, String artifactId, String version, String type,
                              String classifier, boolean transitive) {
        try {
            Artifact artifact = new DefaultArtifact(groupId, artifactId, version,
                    Artifact.SCOPE_RUNTIME, type, classifier,
                    manager.getArtifactHandler(type));
            return resolve(artifact, transitive);
        } catch (MojoExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String buildClassPath(Collection<File> elements) {
        return CampaignUtil.buildClassPath(elements.toArray(new File[0]));
    }

    public List<File> resolve(Artifact artifact) throws MojoExecutionException {
        return resolve(artifact, true);
    }

    public List<File> resolve(Artifact artifact, boolean transitive) throws MojoExecutionException {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest(baseRequest)
                .setArtifact(artifact)
                .setResolveTransitively(transitive)
                .setCollectionFilter(RUNTIME_FILTER);
        ArtifactResolutionResult result = repositorySystem.resolve(request);
        try {
            errorHandler.throwErrors(request, result);
            return result.getArtifacts()
                    .stream()
                    .map(Artifact::getFile)
                    .collect(Collectors.toList());
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}