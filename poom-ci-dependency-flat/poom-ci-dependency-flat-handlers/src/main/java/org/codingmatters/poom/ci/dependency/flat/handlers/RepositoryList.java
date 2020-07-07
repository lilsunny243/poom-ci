package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoriesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoriesGetResponse;
import org.codingmatters.poom.ci.dependency.api.repositoriesgetresponse.Status200;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.function.Function;

public class RepositoryList implements Function<RepositoriesGetRequest, RepositoriesGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RepositoryList.class);
    private final GraphManager graphManager;

    public RepositoryList(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    @Override
    public RepositoriesGetResponse apply(RepositoriesGetRequest request) {
        return RepositoriesGetResponse.builder()
                .status200(Status200.builder()
                        .payload(this.graphManager.repositories())
                        .build())
                .build();
    }
}
