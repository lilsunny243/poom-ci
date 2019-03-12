package org.codingmatters.poom.ci.gremlin.service.handlers;

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.codingmatters.poom.ci.dependency.api.RepositoryDownstreamRepositoriesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryDownstreamRepositoriesGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.gremlin.queries.DownstreamQuery;
import org.codingmatters.poom.ci.gremlin.service.Mappers;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.List;
import java.util.function.Function;

public class ListDownstreams implements Function<RepositoryDownstreamRepositoriesGetRequest, RepositoryDownstreamRepositoriesGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ListDownstreams.class);
    private DriverRemoteConnection connection;

    public ListDownstreams(DriverRemoteConnection connection) {
        this.connection = connection;
    }

    @Override
    public RepositoryDownstreamRepositoriesGetResponse apply(RepositoryDownstreamRepositoriesGetRequest request) {
        List<Repository> results = new DownstreamQuery<>(AnonymousTraversalSource.traversal().withRemote(this.connection), Mappers::repository)
                .forRepository(request.repositoryId());

        return RepositoryDownstreamRepositoriesGetResponse.builder()
                .status200(status -> status.payload(results))
                .build();
    }
}
