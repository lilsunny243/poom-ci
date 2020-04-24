package org.codingmatters.poom.ci.gremlin.service.handlers;

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.codingmatters.poom.ci.dependency.api.RepositoryDeleteRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryDeleteResponse;
import org.codingmatters.poom.ci.dependency.api.repositorydeleteresponse.Status200;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.gremlin.queries.DeleteRepositoryQuery;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.function.Function;
import java.util.function.Supplier;

public class DeleteRepository implements Function<RepositoryDeleteRequest, RepositoryDeleteResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(DeleteRepository.class);

    private Supplier<RemoteConnection> connectionSupplier;

    public DeleteRepository(Supplier<RemoteConnection> connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    @Override
    public RepositoryDeleteResponse apply(RepositoryDeleteRequest request) {
        try(RemoteConnection connection = this.connectionSupplier.get()) {
            new DeleteRepositoryQuery(AnonymousTraversalSource.traversal().withRemote(connection)).delete(request.repositoryId());
            return RepositoryDeleteResponse.builder().status200(Status200.builder().build()).build();
        } catch (Exception e) {
            return RepositoryDeleteResponse.builder()
                    .status500(status -> status.payload(Error.builder().code(Error.Code.UNEXPECTED_ERROR).token(log.tokenized().error("error accessing gremlin server", e)).build()))
                    .build();
        }
    }
}
