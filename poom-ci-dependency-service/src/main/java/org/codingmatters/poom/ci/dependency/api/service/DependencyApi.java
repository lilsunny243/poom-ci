package org.codingmatters.poom.ci.dependency.api.service;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.dependency.api.PoomCIDependencyAPIHandlers;
import org.codingmatters.poom.ci.dependency.api.service.handlers.*;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.Processor;

public class DependencyApi {
    static private CategorizedLogger log = CategorizedLogger.getLogger(DependencyApi.class);

    private final JsonFactory jsonFactory;
    private final String path;
    private final DependencyGraph dependencyGraph;
    private PoomCIDependencyAPIHandlers handlers;

    public DependencyApi(JsonFactory jsonFactory, String path, DependencyGraph dependencyGraph) {
        this.jsonFactory = jsonFactory;
        this.path = path;
        this.dependencyGraph = dependencyGraph;

        this.handlers = new PoomCIDependencyAPIHandlers.Builder()
                .repositoriesGetHandler(new RepositoriesGet(this.dependencyGraph))

                .repositoryGetHandler(new RepositoryGet(this.dependencyGraph))
                .repositoryDependenciesGetHandler(new RepositoryDependenciesGet(this.dependencyGraph))
                .repositoryModulesGetHandler(new RepositoryModulesGet(this.dependencyGraph))

                .repositoryDownstreamRepositoriesGetHandler(new RepositoryDownstreamGet(this.dependencyGraph))
                .repositoryJustNextDownstreamRepositoriesGetHandler(new RepositoryJustNextDownstreamGet(this.dependencyGraph))

                .repositoryPutHandler(new RepositoryPut(this.dependencyGraph))
                .repositoryDependenciesPostHandler(new RepositoryDependencyPost(this.dependencyGraph))
                .repositoryModulesPostHandler(new RepositoryProducesPost(this.dependencyGraph))
                .build();
    }

    public Processor processor() {
        return new PoomCIDependencyAPIProcessor(this.path, this.jsonFactory, this.handlers);
    }

    public String path() {
        return path;
    }
}
