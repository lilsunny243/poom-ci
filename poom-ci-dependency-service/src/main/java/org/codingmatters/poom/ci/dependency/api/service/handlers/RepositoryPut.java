package org.codingmatters.poom.ci.dependency.api.service.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryPutRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryPutResponse;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;

import java.io.IOException;
import java.util.function.Function;

public class RepositoryPut implements Function<RepositoryPutRequest, RepositoryPutResponse> {
    private final DependencyGraph dependencyGraph;

    public RepositoryPut(DependencyGraph dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }

    @Override
    public RepositoryPutResponse apply(RepositoryPutRequest request) {
        if(request.opt().repositoryId().isPresent()) {
            if(request.opt().payload().name().isPresent() && request.opt().payload().checkoutSpec().isPresent()) {
                try {
                    Repository repository = this.dependencyGraph.update(request.payload().withId(request.repositoryId()));
                    return RepositoryPutResponse.builder()
                            .status200(status -> status.payload(repository))
                            .build();
                } catch (IOException e) {
                    return RepositoryPutResponse.builder()
                            .status500(status -> status.payload(error -> error
                                    .code(Error.Code.UNEXPECTED_ERROR).description("error updating repository")))
                            .build();
                }
            } else {
                return RepositoryPutResponse.builder()
                        .status400(status -> status.payload(error -> error
                                .code(Error.Code.ILLEGAL_REQUEST).description("must provide at least name and checkoutSpec")))
                        .build();
            }
        } else {
            return RepositoryPutResponse.builder()
                    .status400(status -> status.payload(error -> error.code(Error.Code.ILLEGAL_REQUEST)))
                    .build();
        }
    }
}
