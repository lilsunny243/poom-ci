package org.codingmatters.poom.ci.dependency.api.service.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryModulesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryModulesGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;

import java.util.Optional;
import java.util.function.Function;

public class RepositoryModulesGet implements Function<RepositoryModulesGetRequest, RepositoryModulesGetResponse> {
    private final DependencyGraph dependencyGraph;

    public RepositoryModulesGet(DependencyGraph dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }

    @Override
    public RepositoryModulesGetResponse apply(RepositoryModulesGetRequest request) {
        if(request.opt().repositoryId().isPresent()) {
            Optional<Repository> repository = this.dependencyGraph.repositoryById(request.repositoryId());
            if(repository.isPresent()) {
                return RepositoryModulesGetResponse.builder()
                        .status200(status -> status.payload(this.dependencyGraph.produced(repository.get())))
                        .build();
            } else {
                return RepositoryModulesGetResponse.builder()
                        .status404(status -> status.payload(error -> error.code(Error.Code.RESOURCE_NOT_FOUND)))
                        .build();
            }
        } else {
            return RepositoryModulesGetResponse.builder()
                    .status400(status -> status.payload(error -> error.code(Error.Code.ILLEGAL_REQUEST)))
                    .build();
        }
    }
}
