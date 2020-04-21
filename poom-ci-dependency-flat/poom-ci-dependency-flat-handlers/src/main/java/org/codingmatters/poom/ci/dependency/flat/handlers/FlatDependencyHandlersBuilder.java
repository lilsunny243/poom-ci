package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.PoomCIDependencyAPIHandlers;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;

public class FlatDependencyHandlersBuilder extends PoomCIDependencyAPIHandlers.Builder {

    public FlatDependencyHandlersBuilder(GraphManager graphManager) {
        this.repositoriesGetHandler(new RepositoryList(graphManager));
        this.repositoryGetHandler(new RepositoryGet(graphManager));
        this.repositoryDeleteHandler(new RepositoryDelete(graphManager));
        this.repositoryDependenciesGetHandler(new RepositoryDependsOn(graphManager));
        this.repositoryModulesGetHandler(new RepositoryProduces(graphManager));
    }
}
