package org.codingmatters.poom.ci.gremlin.service.handlers;

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.codingmatters.poom.ci.dependency.api.RepositoryDependenciesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryDependenciesGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.gremlin.queries.DependenciesQuery;
import org.codingmatters.poom.ci.gremlin.service.Mappers;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ListDependencies implements Function<RepositoryDependenciesGetRequest, RepositoryDependenciesGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ListDependencies.class);

    private Supplier<RemoteConnection> connectionSupplier;

    public ListDependencies(Supplier<RemoteConnection> connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    @Override
    public RepositoryDependenciesGetResponse apply(RepositoryDependenciesGetRequest request) {
        try(RemoteConnection connection = this.connectionSupplier.get()) {
            List<Module> results = new DependenciesQuery<>(AnonymousTraversalSource.traversal().withRemote(connection), Mappers::module)
                    .forRepository(request.repositoryId());
            return RepositoryDependenciesGetResponse.builder()
                    .status200(status -> status.payload(results))
                    .build();
        } catch (Exception e) {
            return RepositoryDependenciesGetResponse.builder()
                    .status500(status -> status.payload(Error.builder().code(Error.Code.UNEXPECTED_ERROR).token(log.tokenized().error("error accessing gremlin server", e)).build()))
                    .build();
        }
    }
}
