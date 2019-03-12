package org.codingmatters.poom.ci.gremlin.service.handlers;

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.codingmatters.poom.ci.dependency.api.RepositoryDependenciesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryDependenciesGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.gremlin.queries.DependenciesQuery;
import org.codingmatters.poom.ci.gremlin.service.Mappers;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.List;
import java.util.function.Function;

public class ListDependencies implements Function<RepositoryDependenciesGetRequest, RepositoryDependenciesGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ListDependencies.class);

    private DriverRemoteConnection connection;

    public ListDependencies(DriverRemoteConnection connection) {
        this.connection = connection;
    }

    @Override
    public RepositoryDependenciesGetResponse apply(RepositoryDependenciesGetRequest request) {
        List<Module> results = new DependenciesQuery<>(AnonymousTraversalSource.traversal().withRemote(this.connection), Mappers::module)
                .forRepository(request.repositoryId());
        return RepositoryDependenciesGetResponse.builder()
                .status200(status -> status.payload(results))
                .build();
    }
}
