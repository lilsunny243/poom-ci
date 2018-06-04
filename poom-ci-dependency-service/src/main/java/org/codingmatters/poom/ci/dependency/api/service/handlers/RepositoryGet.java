package org.codingmatters.poom.ci.dependency.api.service.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;

import java.util.Optional;
import java.util.function.Function;

public class RepositoryGet implements Function<RepositoryGetRequest, RepositoryGetResponse> {
    private final DependencyGraph dependencyGraph;

    public RepositoryGet(DependencyGraph dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }

    @Override
    public RepositoryGetResponse apply(RepositoryGetRequest request) {
        if(request.opt().repositoryId().isPresent()) {
            Optional<Repository> repo = this.dependencyGraph.repositoryById(request.opt().repositoryId().get());
            if(repo.isPresent()) {
                return RepositoryGetResponse.builder()
                        .status200(status -> status.payload(repo.get()))
                        .build();
            } else {
                return RepositoryGetResponse.builder().status404(status -> status.payload(error -> error
                        .code(Error.Code.RESOURCE_NOT_FOUND)))
                        .build();
            }
        } else {
            return RepositoryGetResponse.builder()
                    .status400(status -> status.payload(error -> error
                    .code(Error.Code.ILLEGAL_REQUEST)))
                    .build();
        }
    }
}
