package org.codingmatters.poom.ci.dependency.flat;

import org.codingmatters.poom.ci.dependency.api.types.Repository;

import java.util.Optional;

public class DownstreamProcessor {
    private final GraphManager graphManager;

    public DownstreamProcessor(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    public Repository[] downstream(String repositoryId) throws NoSuchRepositoryException, GraphManagerException {
        Optional<Repository> repository = this.graphManager.repository(repositoryId);
        if(! repository.isPresent()) {
            throw new NoSuchRepositoryException("no repository with id " + repositoryId);
        }
        return new Repository[0];
    }
}
