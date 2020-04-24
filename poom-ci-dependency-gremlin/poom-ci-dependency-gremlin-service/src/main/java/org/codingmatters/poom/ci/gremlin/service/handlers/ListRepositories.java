package org.codingmatters.poom.ci.gremlin.service.handlers;

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.codingmatters.poom.ci.dependency.api.RepositoriesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoriesGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.gremlin.queries.RepositoryQuery;
import org.codingmatters.poom.ci.gremlin.service.Mappers;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ListRepositories implements Function<RepositoriesGetRequest, RepositoriesGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ListRepositories.class);

    private final Supplier<RemoteConnection> connectionSupplier;

    public ListRepositories(Supplier<RemoteConnection> connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    @Override
    public RepositoriesGetResponse apply(RepositoriesGetRequest request) {
        try(RemoteConnection connection = this.connectionSupplier.get()) {
            List<Repository> results = new RepositoryQuery<>(AnonymousTraversalSource.traversal().withRemote(connection), Mappers::repository)
                    .all();

            return RepositoriesGetResponse.builder()
                    .status200(status -> status.payload(results))
                    .build();
        } catch (Exception e) {
            return RepositoriesGetResponse.builder()
                    .status500(status -> status.payload(Error.builder().code(Error.Code.UNEXPECTED_ERROR).token(log.tokenized().error("error accessing gremlin server", e)).build()))
                    .build();
        }
    }
}
