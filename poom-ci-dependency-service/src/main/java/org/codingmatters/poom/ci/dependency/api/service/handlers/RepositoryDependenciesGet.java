package org.codingmatters.poom.ci.dependency.api.service.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryDependenciesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryDependenciesGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;

import java.util.Optional;
import java.util.function.Function;

public class RepositoryDependenciesGet implements Function<RepositoryDependenciesGetRequest, RepositoryDependenciesGetResponse> {
    private final DependencyGraph dependencyGraph;

    public RepositoryDependenciesGet(DependencyGraph dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }

    @Override
    public RepositoryDependenciesGetResponse apply(RepositoryDependenciesGetRequest request) {

        if(request.opt().repositoryId().isPresent()) {
            Optional<Repository> repository = this.dependencyGraph.repositoryById(request.repositoryId());
            if(repository.isPresent()) {
                return RepositoryDependenciesGetResponse.builder()
                        .status200(status -> status.payload(this.dependencyGraph.dependencies(repository.get())))
                        .build();
            } else {
                return RepositoryDependenciesGetResponse.builder().status404(status -> status.payload(error -> error
                    .code(Error.Code.RESOURCE_NOT_FOUND)))
                    .build();
            }
        } else {
            return RepositoryDependenciesGetResponse.builder().status400(status -> status.payload(error -> error
                    .code(Error.Code.ILLEGAL_REQUEST)))
                    .build();
        }
    }
}
