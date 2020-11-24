package org.codingmatters.poom.ci.dependency.flat;

import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.flat.downstream.RelationProcessorWalker;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;

public class PropagationCandidatesProcessor implements RelationProcessor {
    public enum Restriction {
        NONE, FIRST_LEVEL
    }

    private final GraphManager graphManager;
    private final Restriction restriction;

    public PropagationCandidatesProcessor(GraphManager graphManager, Restriction restriction) {
        this.graphManager = graphManager;
        this.restriction = restriction;
    }

    public Repository[] all(String repositoryId) throws NoSuchRepositoryException, GraphManagerException {
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

    public Repository[] firstLevel(String repositoryId) throws NoSuchRepositoryException, GraphManagerException {
        LinkedList<Repository> result = new LinkedList<>();
        new RelationProcessorWalker(this.graphManager, repoId -> this.all(repoId), (parent, downstream, cycleInduced) -> {
            if(! cycleInduced) {
                if(parent.id().equals(repositoryId)) {https://trello.com/c/hflny4it/41-bridge-corrections-api
                    result.add(downstream);
                } else if(result.contains(downstream)) {
                    // downstream is downstream of a downstream, it is not a first level downstream
                    result.remove(downstream);
                }
            }
        }).startFrom(repositoryId);
        return result.toArray(new Repository[0]);
    }

    @Override
    public Repository[] process(String repositoryId) throws NoSuchRepositoryException, GraphManagerException {
        switch (this.restriction) {
            case NONE:
                return this.all(repositoryId);
            case FIRST_LEVEL:
                return this.firstLevel(repositoryId);
        }
        return new Repository[0];
    }
}
