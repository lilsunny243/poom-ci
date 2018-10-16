package org.codingmatters.poom.ci.dependency.api.service;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.dependency.api.PoomCIDependencyAPIHandlers;
import org.codingmatters.poom.ci.dependency.api.service.handlers.*;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.Processor;

import static org.codingmatters.poom.ci.dependency.api.service.handlers.WithLock.locked;

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
                .repositoriesGetHandler(locked(new RepositoriesGet(this.dependencyGraph), this.dependencyGraph))

                .repositoryGetHandler(locked(new RepositoryGet(this.dependencyGraph), this.dependencyGraph))
                .repositoryPutHandler(locked(new RepositoryPut(this.dependencyGraph), this.dependencyGraph))
                .repositoryDeleteHandler(locked(new RepositoryDelete(this.dependencyGraph), this.dependencyGraph))

                .repositoryDependenciesGetHandler(locked(new RepositoryDependenciesGet(this.dependencyGraph), this.dependencyGraph))
                .repositoryModulesGetHandler(locked(new RepositoryModulesGet(this.dependencyGraph), this.dependencyGraph))

                .repositoryDownstreamRepositoriesGetHandler(locked(new RepositoryDownstreamGet(this.dependencyGraph), this.dependencyGraph))
                .repositoryJustNextDownstreamRepositoriesGetHandler(locked(new RepositoryJustNextDownstreamGet(this.dependencyGraph), this.dependencyGraph))

                .repositoryDependenciesPostHandler(locked(new RepositoryDependencyPost(this.dependencyGraph), this.dependencyGraph))
                .repositoryModulesPostHandler(locked(new RepositoryProducesPost(this.dependencyGraph), this.dependencyGraph))
                .build();
    }

    public Processor processor() {
        return new PoomCIDependencyAPIProcessor(this.path, this.jsonFactory, this.handlers);
    }

    public String path() {
        return path;
    }
}
