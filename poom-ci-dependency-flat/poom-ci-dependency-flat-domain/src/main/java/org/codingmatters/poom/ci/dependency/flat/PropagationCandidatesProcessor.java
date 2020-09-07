package org.codingmatters.poom.ci.dependency.flat;

import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;

import java.util.HashSet;
import java.util.Optional;

public class PropagationCandidatesProcessor {
    private final GraphManager graphManager;

    public PropagationCandidatesProcessor(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    public Repository[] candidates(String repositoryId) throws NoSuchRepositoryException, GraphManagerException {

        Optional<Repository> repository = this.graphManager.repository(repositoryId);
        if(repository.isEmpty()) throw new NoSuchRepositoryException("repository not found : " + repositoryId);

        HashSet<Repository> result = new HashSet<>();
        for (Module module : this.graphManager.producedBy(repository.get())) {
            for (Repository dependent : this.graphManager.dependentOnSpecRepositories(module)) {
                result.add(dependent);
            }
        }

        return result.toArray(new Repository[0]);
    }
}
