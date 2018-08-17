package org.codingmatters.poom.ci.dependency.api.service.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryJustNextDownstreamRepositoriesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryJustNextDownstreamRepositoriesGetResponse;
import org.codingmatters.poom.ci.dependency.api.repositoryjustnextdownstreamrepositoriesgetresponse.Status200;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

public class RepositoryJustNextDownstreamGet implements Function<RepositoryJustNextDownstreamRepositoriesGetRequest, RepositoryJustNextDownstreamRepositoriesGetResponse> {
    private final DependencyGraph dependencyGraph;

    public RepositoryJustNextDownstreamGet(DependencyGraph dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }

    @Override
    public RepositoryJustNextDownstreamRepositoriesGetResponse apply(RepositoryJustNextDownstreamRepositoriesGetRequest request) {
        if(request.opt().repositoryId().isPresent()) {
            Optional<Repository> repository = this.dependencyGraph.repositoryById(request.repositoryId());
            if(repository.isPresent()) {
                try {
                    return RepositoryJustNextDownstreamRepositoriesGetResponse.builder()
                            .status200(Status200.builder()
                                    .payload(this.dependencyGraph
                                            .downstreamGraph(repository.get())
                                            .dependencyTreeFirstSteps(repository.get()))
                                    .build())
                            .build();
                } catch (IOException e) {
                    return RepositoryJustNextDownstreamRepositoriesGetResponse.builder()
                            .status500(status -> status.payload(error -> error.code(Error.Code.UNEXPECTED_ERROR)))
                            .build();
                }
            } else {
                return RepositoryJustNextDownstreamRepositoriesGetResponse.builder()
                        .status404(status -> status.payload(error -> error.code(Error.Code.RESOURCE_NOT_FOUND)))
                        .build();
            }
        } else {
            return RepositoryJustNextDownstreamRepositoriesGetResponse.builder()
                    .status400(status -> status.payload(error -> error.code(Error.Code.ILLEGAL_REQUEST)))
                    .build();
        }
    }
}
