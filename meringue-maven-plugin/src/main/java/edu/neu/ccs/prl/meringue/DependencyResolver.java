package edu.neu.ccs.prl.meringue;

import org.apache.maven.artifact.Artifact;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class DependencyResolver {
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
    private final RepositoryRequest baseRequest;

    public DependencyResolver(RepositorySystem repositorySystem, ArtifactRepository localRepository,
                              List<ArtifactRepository> remoteRepositories,
                              ResolutionErrorHandler errorHandler, boolean offline) {
        this.repositorySystem = repositorySystem;
        this.errorHandler = errorHandler;
        this.baseRequest = new ArtifactResolutionRequest()
                .setOffline(offline)
                .setLocalRepository(localRepository)
                .setRemoteRepositories(remoteRepositories);
    }

    public List<File> resolve(Artifact artifact) throws MojoExecutionException {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest(baseRequest)
                .setArtifact(artifact)
                .setResolveTransitively(true)
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