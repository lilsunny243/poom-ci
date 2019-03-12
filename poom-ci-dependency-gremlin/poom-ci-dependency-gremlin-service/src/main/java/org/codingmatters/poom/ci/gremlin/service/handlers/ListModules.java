package org.codingmatters.poom.ci.gremlin.service.handlers;

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.codingmatters.poom.ci.dependency.api.RepositoryModulesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryModulesGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.gremlin.queries.ProducedByQuery;
import org.codingmatters.poom.ci.gremlin.service.Mappers;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.List;
import java.util.function.Function;

public class ListModules implements Function<RepositoryModulesGetRequest, RepositoryModulesGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ListModules.class);

    private DriverRemoteConnection connection;

    public ListModules(DriverRemoteConnection connection) {
        this.connection = connection;
    }

    @Override
    public RepositoryModulesGetResponse apply(RepositoryModulesGetRequest request) {
        List<Module> results = new ProducedByQuery<>(AnonymousTraversalSource.traversal().withRemote(this.connection), Mappers::module)
                .forRepository(request.repositoryId());

        return RepositoryModulesGetResponse.builder()
                .status200(status -> status.payload(results))
                .build();
    }
}
