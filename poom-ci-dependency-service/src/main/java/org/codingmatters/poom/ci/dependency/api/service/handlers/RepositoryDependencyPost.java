package org.codingmatters.poom.ci.dependency.api.service.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryDependenciesPostRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryDependenciesPostResponse;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;
import org.codingmatters.poom.ci.dependency.graph.tinkerpop.TinkerPopDependencyGraph;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

public class RepositoryDependencyPost implements Function<RepositoryDependenciesPostRequest, RepositoryDependenciesPostResponse> {
    private final DependencyGraph dependencyGraph;

    public RepositoryDependencyPost(DependencyGraph dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }

    @Override
    public RepositoryDependenciesPostResponse apply(RepositoryDependenciesPostRequest request) {
        if(request.opt().repositoryId().isPresent()) {
            Optional<Repository> repository = this.dependencyGraph.repositoryById(request.repositoryId());
            if(repository.isPresent()) {
                this.dependencyGraph.resetDependencies(repository.get());
                if(request.opt().payload().isPresent() && ! request.payload().isEmpty()) {
                    try {
                        this.dependencyGraph.dependsOn(repository.get(), request.payload().toArray(new Module[request.payload().size()]));
                    } catch (IOException e) {
                        return RepositoryDependenciesPostResponse.builder()
                                .status500(status -> status.payload(error -> error.code(Error.Code.UNEXPECTED_ERROR)))
                                .build();
                    }
                }
                return RepositoryDependenciesPostResponse.builder()
                        .status201(status -> status.location(String.format(
                                "/repositories/%s/depends-on", repository.get().id()
                        )))
                        .build();
            } else {
                return RepositoryDependenciesPostResponse.builder()
                        .status404(status -> status.payload(error -> error.code(Error.Code.RESOURCE_NOT_FOUND)))
                        .build();
            }
        } else {
            return RepositoryDependenciesPostResponse.builder()
                    .status400(status -> status.payload(error -> error.code(Error.Code.ILLEGAL_REQUEST)))
                    .build();
        }
    }
}
