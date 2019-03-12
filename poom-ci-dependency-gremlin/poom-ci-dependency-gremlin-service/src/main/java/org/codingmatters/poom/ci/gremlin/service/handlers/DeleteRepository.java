package org.codingmatters.poom.ci.gremlin.service.handlers;

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.codingmatters.poom.ci.dependency.api.RepositoryDeleteRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryDeleteResponse;
import org.codingmatters.poom.ci.dependency.api.repositorydeleteresponse.Status200;
import org.codingmatters.poom.ci.gremlin.queries.DeleteRepositoryQuery;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.function.Function;

public class DeleteRepository implements Function<RepositoryDeleteRequest, RepositoryDeleteResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(DeleteRepository.class);

    private DriverRemoteConnection connection;

    public DeleteRepository(DriverRemoteConnection connection) {
        this.connection = connection;
    }

    @Override
    public RepositoryDeleteResponse apply(RepositoryDeleteRequest request) {
        new DeleteRepositoryQuery(AnonymousTraversalSource.traversal().withRemote(this.connection)).delete(request.repositoryId());
        return RepositoryDeleteResponse.builder().status200(Status200.builder().build()).build();
    }
}
