package org.codingmatters.poom.ci.dependency.api.service.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryDownstreamRepositoriesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryDownstreamRepositoriesGetResponse;
import org.codingmatters.poom.ci.dependency.api.repositorydownstreamrepositoriesgetresponse.Status200;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;
import org.codingmatters.poom.ci.dependency.graph.tinkerpop.TinkerPopDependencyGraph;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

public class RepositoryDownstreamGet implements Function<RepositoryDownstreamRepositoriesGetRequest, RepositoryDownstreamRepositoriesGetResponse> {
    private final DependencyGraph dependencyGraph;

    public RepositoryDownstreamGet(DependencyGraph dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }

    @Override
    public RepositoryDownstreamRepositoriesGetResponse apply(RepositoryDownstreamRepositoriesGetRequest request) {
        if(request.opt().repositoryId().isPresent()) {
            Optional<Repository> repository = this.dependencyGraph.repositoryById(request.repositoryId());
            if(repository.isPresent()) {
                try {
                    return RepositoryDownstreamRepositoriesGetResponse.builder()
                            .status200(Status200.builder()
                                    .payload(this.dependencyGraph
                                            .downstreamGraph(repository.get())
                                            .direct(repository.get()))
                                    .build())
                            .build();
                } catch (IOException e) {
                    return RepositoryDownstreamRepositoriesGetResponse.builder()
                            .status500(status -> status.payload(error -> error.code(Error.Code.UNEXPECTED_ERROR)))
                            .build();
                }
            } else {
                return RepositoryDownstreamRepositoriesGetResponse.builder()
                        .status404(status -> status.payload(error -> error.code(Error.Code.RESOURCE_NOT_FOUND)))
                        .build();
            }
        } else {
            return RepositoryDownstreamRepositoriesGetResponse.builder()
                    .status400(status -> status.payload(error -> error.code(Error.Code.ILLEGAL_REQUEST)))
                    .build();
        }
    }
}
