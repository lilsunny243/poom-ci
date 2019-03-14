package org.codingmatters.poom.ci.gremlin.service.handlers;

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.codingmatters.poom.ci.dependency.api.RepositoriesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoriesGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.gremlin.queries.RepositoryQuery;
import org.codingmatters.poom.ci.gremlin.service.Mappers;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.List;
import java.util.function.Function;

public class ListRepositories implements Function<RepositoriesGetRequest, RepositoriesGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ListRepositories.class);

    private final DriverRemoteConnection connection;

    public ListRepositories(DriverRemoteConnection connection) {
        this.connection = connection;
    }

    @Override
    public RepositoriesGetResponse apply(RepositoriesGetRequest request) {
        List<Repository> results = new RepositoryQuery<>(AnonymousTraversalSource.traversal().withRemote(this.connection), Mappers::repository)
                .all();

        return RepositoriesGetResponse.builder()
                .status200(status -> status.payload(results))
                .build();
    }
}
