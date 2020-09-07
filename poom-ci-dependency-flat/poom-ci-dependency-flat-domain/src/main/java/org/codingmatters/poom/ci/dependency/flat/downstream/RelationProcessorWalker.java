package org.codingmatters.poom.ci.dependency.flat.downstream;

import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.flat.*;

import java.util.HashSet;
import java.util.Optional;

public class RelationProcessorWalker {

    private final GraphManager graphManager;
    private final DownstreamWalkerListener listener;
    private final RelationProcessor relationProcessor;

    private final ThreadLocal<HashSet<String>> alreadySeen = new ThreadLocal<>() {
        @Override
        protected HashSet<String> initialValue() {
            return new HashSet<>();
        }
    };

    public RelationProcessorWalker(GraphManager graphManager, RelationProcessor relationProcessor, DownstreamWalkerListener listener) {
        this.graphManager = graphManager;
        this.listener = listener;
        this.relationProcessor = relationProcessor;
    }

    public void startFrom(String repositoryId) throws GraphManagerException, NoSuchRepositoryException {
        Optional<Repository> repository = this.graphManager.repository(repositoryId);
        if(! repository.isPresent()) throw new NoSuchRepositoryException("no repository found with id " + repositoryId);

        this.alreadySeen.get().clear();
        this.walk(repository.get());
    }

    private void walk(Repository repository) throws NoSuchRepositoryException, GraphManagerException {
        this.alreadySeen.get().add(repository.id());
        for (Repository related : this.relationProcessor.process(repository.id())) {
            if(this.alreadySeen.get().contains(related.id())) {
                this.listener.hasRelated(repository, related, true);
            } else {
                this.listener.hasRelated(repository, related, false);
                this.walk(related);
            }
        }
    }
}
