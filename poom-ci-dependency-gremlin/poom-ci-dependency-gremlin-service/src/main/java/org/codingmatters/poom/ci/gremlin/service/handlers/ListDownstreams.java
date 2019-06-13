package org.codingmatters.poom.ci.gremlin.service.handlers;

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.codingmatters.poom.ci.dependency.api.RepositoryDependenciesGetResponse;
import org.codingmatters.poom.ci.dependency.api.RepositoryDownstreamRepositoriesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryDownstreamRepositoriesGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.gremlin.queries.DownstreamQuery;
import org.codingmatters.poom.ci.gremlin.service.Mappers;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ListDownstreams implements Function<RepositoryDownstreamRepositoriesGetRequest, RepositoryDownstreamRepositoriesGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ListDownstreams.class);
    private Supplier<RemoteConnection> connectionSupplier;

    public ListDownstreams(Supplier<RemoteConnection> connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    @Override
    public RepositoryDownstreamRepositoriesGetResponse apply(RepositoryDownstreamRepositoriesGetRequest request) {
        try(RemoteConnection connection = this.connectionSupplier.get()) {
            List<Repository> results = new DownstreamQuery<>(AnonymousTraversalSource.traversal().withRemote(connection), Mappers::repository)
                    .forRepository(request.repositoryId());

            return RepositoryDownstreamRepositoriesGetResponse.builder()
                    .status200(status -> status.payload(results))
                    .build();
        } catch (Exception e) {
            return RepositoryDownstreamRepositoriesGetResponse.builder()
                    .status500(status -> status.payload(Error.builder().code(Error.Code.UNEXPECTED_ERROR).token(log.tokenized().error("error accessing gremlin server", e)).build()))
                    .build();
        }
    }
}
