package org.codingmatters.poom.ci.dependency.api.service.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryModulesPostRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryModulesPostResponse;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

public class RepositoryProducesPost implements Function<RepositoryModulesPostRequest, RepositoryModulesPostResponse> {
    private final DependencyGraph dependencyGraph;

    public RepositoryProducesPost(DependencyGraph dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }

    @Override
    public RepositoryModulesPostResponse apply(RepositoryModulesPostRequest request) {
        if(request.opt().repositoryId().isPresent()) {
            Optional<Repository> repository = this.dependencyGraph.repositoryById(request.repositoryId());
            if(repository.isPresent()) {
                this.dependencyGraph.resetProduced(repository.get());
                if(request.opt().payload().isPresent() && ! request.payload().isEmpty()) {
                    try {
                        this.dependencyGraph.produces(repository.get(), request.payload().toArray(new Module[request.payload().size()]));
                    } catch (IOException e) {
                        return RepositoryModulesPostResponse.builder()
                                .status500(status -> status.payload(error -> error.code(Error.Code.UNEXPECTED_ERROR)))
                                .build();
                    }
                }
                return RepositoryModulesPostResponse.builder()
                        .status201(status -> status.location(String.format(
                                "/repositories/%s/produces", repository.get().id()
                        )))
                        .build();
            } else {
                return RepositoryModulesPostResponse.builder()
                        .status404(status -> status.payload(error -> error.code(Error.Code.RESOURCE_NOT_FOUND)))
                        .build();
            }
        } else {
            return RepositoryModulesPostResponse.builder()
                    .status400(status -> status.payload(error -> error.code(Error.Code.ILLEGAL_REQUEST)))
                    .build();
        }
    }
}
