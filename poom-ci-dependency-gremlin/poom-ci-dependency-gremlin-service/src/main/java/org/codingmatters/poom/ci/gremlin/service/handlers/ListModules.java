package org.codingmatters.poom.ci.gremlin.service.handlers;

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.codingmatters.poom.ci.dependency.api.RepositoryGetResponse;
import org.codingmatters.poom.ci.dependency.api.RepositoryModulesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryModulesGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.gremlin.queries.ProducedByQuery;
import org.codingmatters.poom.ci.gremlin.service.Mappers;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ListModules implements Function<RepositoryModulesGetRequest, RepositoryModulesGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ListModules.class);

    private Supplier<RemoteConnection> connectionSupplier;

    public ListModules(Supplier<RemoteConnection> connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    @Override
    public RepositoryModulesGetResponse apply(RepositoryModulesGetRequest request) {
        try(RemoteConnection connection = this.connectionSupplier.get()) {
            List<Module> results = new ProducedByQuery<>(AnonymousTraversalSource.traversal().withRemote(connection), Mappers::module)
                    .forRepository(request.repositoryId());

            return RepositoryModulesGetResponse.builder()
                    .status200(status -> status.payload(results))
                    .build();
        } catch (Exception e) {
            return RepositoryModulesGetResponse.builder()
                    .status500(status -> status.payload(Error.builder().code(Error.Code.UNEXPECTED_ERROR).token(log.tokenized().error("error accessing gremlin server", e)).build()))
                    .build();
        }
    }
}
