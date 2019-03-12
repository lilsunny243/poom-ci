package org.codingmatters.poom.ci.gremlin.service.handlers;

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.codingmatters.poom.ci.dependency.api.RepositoryJustNextDownstreamRepositoriesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryJustNextDownstreamRepositoriesGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.gremlin.queries.NextDownstreamsQuery;
import org.codingmatters.poom.ci.gremlin.service.Mappers;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.List;
import java.util.function.Function;

public class ListNextDownstreams implements Function<RepositoryJustNextDownstreamRepositoriesGetRequest, RepositoryJustNextDownstreamRepositoriesGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ListNextDownstreams.class);
    private DriverRemoteConnection connection;

    public ListNextDownstreams(DriverRemoteConnection connection) {
        this.connection = connection;
    }

    @Override
    public RepositoryJustNextDownstreamRepositoriesGetResponse apply(RepositoryJustNextDownstreamRepositoriesGetRequest request) {
        List<Repository> results = new NextDownstreamsQuery<>(AnonymousTraversalSource.traversal().withRemote(this.connection), Mappers::repository)
                .forRepository(request.repositoryId());

        return RepositoryJustNextDownstreamRepositoriesGetResponse.builder()
                .status200(status -> status.payload(results))
                .build();
    }
}
