package org.codingmatters.poom.ci.dependency.flat;

import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;

import java.util.*;

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

        HashSet<Repository> result = new HashSet<>();

        for (Module module : this.graphManager.producedBy(repository.get())) {
            for (Repository dependentRepository : this.graphManager.dependentRepositories(module)) {
                result.add(dependentRepository);
            }
        }

        return result.toArray(new Repository[0]);
    }
}
