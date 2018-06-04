package org.codingmatters.poom.ci.dependency.api.service.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoriesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoriesGetResponse;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;

import java.util.function.Function;

public class RepositoriesGet implements Function<RepositoriesGetRequest, RepositoriesGetResponse> {
    private final DependencyGraph dependencyGraph;

    public RepositoriesGet(DependencyGraph dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }

    @Override
    public RepositoriesGetResponse apply(RepositoriesGetRequest repositoriesGetRequest) {
        return RepositoriesGetResponse.builder()
                .status200(status -> status.payload(this.dependencyGraph.repositories()))
                .build();
    }
}
