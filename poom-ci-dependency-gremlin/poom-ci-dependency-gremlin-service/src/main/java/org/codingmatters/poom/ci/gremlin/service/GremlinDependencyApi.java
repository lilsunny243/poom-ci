package org.codingmatters.poom.ci.gremlin.service;

import com.fasterxml.jackson.core.JsonFactory;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.codingmatters.poom.ci.dependency.api.PoomCIDependencyAPIDescriptor;
import org.codingmatters.poom.ci.dependency.api.PoomCIDependencyAPIHandlers;
import org.codingmatters.poom.ci.dependency.api.service.PoomCIDependencyAPIProcessor;
import org.codingmatters.poom.ci.gremlin.service.handlers.*;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.Processor;

import java.util.function.Function;
import java.util.function.Supplier;

public class GremlinDependencyApi {
    static private CategorizedLogger log = CategorizedLogger.getLogger(GremlinDependencyApi.class);

    private final JsonFactory jsonFactory;
    private final Supplier<RemoteConnection> connectionSupplier;
    private PoomCIDependencyAPIHandlers handlers;

    public GremlinDependencyApi(JsonFactory jsonFactory, Supplier<RemoteConnection> connectionSupplier) {
        this.jsonFactory = jsonFactory;
        this.connectionSupplier = connectionSupplier;

        this.handlers = new PoomCIDependencyAPIHandlers.Builder()
                .repositoriesGetHandler(new ListRepositories(this.connectionSupplier))
                .repositoryGetHandler(new GetRepository(this.connectionSupplier))
                .repositoryModulesGetHandler(new ListModules(this.connectionSupplier))
                .repositoryDependenciesGetHandler(new ListDependencies(this.connectionSupplier))
                .repositoryDownstreamRepositoriesGetHandler(new ListDownstreams(this.connectionSupplier))
                .repositoryJustNextDownstreamRepositoriesGetHandler(new ListNextDownstreams(this.connectionSupplier))

                .repositoryPutHandler(new CreateOrUpdateRepository(this.connectionSupplier))
                .repositoryDeleteHandler(new DeleteRepository(this.connectionSupplier))

                .repositoryGraphGetHandler(new GraphGet(this.connectionSupplier))

                .build();
    }

    public Processor processor() {
        return new PoomCIDependencyAPIProcessor("/" + PoomCIDependencyAPIDescriptor.NAME, this.jsonFactory, this.handlers);
    }
}
