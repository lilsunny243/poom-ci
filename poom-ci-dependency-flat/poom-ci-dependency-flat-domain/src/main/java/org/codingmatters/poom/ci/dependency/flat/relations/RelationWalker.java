package org.codingmatters.poom.ci.dependency.flat.relations;

import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;
import org.codingmatters.poom.ci.dependency.flat.GraphManagerException;
import org.codingmatters.poom.ci.dependency.flat.NoSuchRepositoryException;

import java.util.HashSet;
import java.util.Optional;

public class RelationWalker {
    private final GraphManager graphManager;
    private final RelationWalkerListener listener;

    private final ThreadLocal<HashSet<String>> alreadySeen = new ThreadLocal<>() {
        @Override
        protected HashSet<String> initialValue() {
            return new HashSet<>();
        }
    };

    public RelationWalker(GraphManager graphManager, RelationWalkerListener listener) {
        this.graphManager = graphManager;
        this.listener = listener;
    }

    public void startFrom(String repositoryId) throws GraphManagerException, NoSuchRepositoryException {
        Optional<Repository> repository = this.graphManager.repository(repositoryId);
        if(! repository.isPresent()) throw new NoSuchRepositoryException("repository not found : " + repositoryId);

        this.alreadySeen.get().clear();
        this.walk(repository.get());
    }

    private void walk(Repository upstream) throws GraphManagerException {
        this.alreadySeen.get().add(upstream.id());
        for (Module module : this.graphManager.producedBy(upstream)) {
            for (Repository downstream : this.graphManager.dependentRepositories(module)) {
                this.listener.relates(upstream, module, downstream);
                if(! this.alreadySeen.get().contains(downstream.id())) {
                    this.walk(downstream);
                }
            }
        }

    }
}
