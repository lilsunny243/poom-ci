package org.codingmatters.poom.ci.dependency.flat.downstream;

import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.flat.DownstreamProcessor;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;
import org.codingmatters.poom.ci.dependency.flat.GraphManagerException;
import org.codingmatters.poom.ci.dependency.flat.NoSuchRepositoryException;

import java.util.HashSet;
import java.util.Optional;

public class DownstreamWalker {

    private final GraphManager graphManager;
    private final DownstreamWalkerListener listener;
    private final DownstreamProcessor downstreamProcessor;

    private final ThreadLocal<HashSet<String>> alreadySeen = new ThreadLocal<>() {
        @Override
        protected HashSet<String> initialValue() {
            return new HashSet<>();
        }
    };

    public DownstreamWalker(GraphManager graphManager, DownstreamWalkerListener listener) {
        this.graphManager = graphManager;
        this.listener = listener;
        this.downstreamProcessor = new DownstreamProcessor(this.graphManager);
    }

    public void startFrom(String repositoryId) throws GraphManagerException, NoSuchRepositoryException {
        Optional<Repository> repository = this.graphManager.repository(repositoryId);
        if(! repository.isPresent()) throw new NoSuchRepositoryException("no repository found with id " + repositoryId);

        this.alreadySeen.get().clear();
        this.walkDownstreams(repository.get());
    }

    public void walkDownstreams(Repository repository) throws NoSuchRepositoryException, GraphManagerException {
        this.alreadySeen.get().add(repository.id());
        for (Repository downstream : this.downstreamProcessor.downstream(repository.id())) {
            if(this.alreadySeen.get().contains(downstream.id())) {
                this.listener.hasDownstream(repository, downstream, true);
            } else {
                this.listener.hasDownstream(repository, downstream, false);
                this.walkDownstreams(downstream);
            }
        }
    }
}
