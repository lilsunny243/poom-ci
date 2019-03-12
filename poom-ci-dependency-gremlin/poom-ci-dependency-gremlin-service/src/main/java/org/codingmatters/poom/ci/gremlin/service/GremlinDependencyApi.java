package org.codingmatters.poom.ci.gremlin.service;

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.shaded.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.dependency.api.PoomCIDependencyAPIHandlers;
import org.codingmatters.poom.ci.gremlin.service.handlers.*;
import org.codingmatters.poom.services.logging.CategorizedLogger;

public class GremlinDependencyApi {
    static private CategorizedLogger log = CategorizedLogger.getLogger(GremlinDependencyApi.class);

    private final JsonFactory jsonFactory;
    private final String path;
    private final DriverRemoteConnection connection;
    private PoomCIDependencyAPIHandlers handlers;

    public GremlinDependencyApi(JsonFactory jsonFactory, String path, DriverRemoteConnection connection) {
        this.jsonFactory = jsonFactory;
        this.path = path;
        this.connection = connection;

        this.handlers = new PoomCIDependencyAPIHandlers.Builder()
                .repositoriesGetHandler(new ListRepositories(this.connection))
                .repositoryGetHandler(new GetRepository(this.connection))
                .repositoryModulesGetHandler(new ListModules(this.connection))
                .repositoryDependenciesGetHandler(new ListDependencies(this.connection))
                .repositoryDownstreamRepositoriesGetHandler(new ListDownstreams(this.connection))
                .repositoryJustNextDownstreamRepositoriesGetHandler(new ListNextDownstreams(this.connection))

                .repositoryPutHandler(new CreateOrUpdateRepository(this.connection))
                .repositoryDeleteHandler(new DeleteRepository(this.connection))

                .build();
    }
}
