package org.codingmatters.poom.ci.gremlin.service.handlers;

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.codingmatters.poom.ci.dependency.api.RepositoryPutRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryPutResponse;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.function.Function;

public class CreateOrUpdateRepository implements Function<RepositoryPutRequest, RepositoryPutResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(CreateOrUpdateRepository.class);

    private DriverRemoteConnection connection;

    public CreateOrUpdateRepository(DriverRemoteConnection connection) {
        this.connection = connection;
    }

    @Override
    public RepositoryPutResponse apply(RepositoryPutRequest request) {
        return null;
    }
}
