package org.codingmatters.poom.ci.dependency.flat;

import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.flat.downstream.DownstreamWalker;

import java.util.LinkedList;

public class FirstLevelDownstreamProcessor {
    private final GraphManager graphManager;

    public FirstLevelDownstreamProcessor(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    public Repository[] downstream(String repositoryId) throws NoSuchRepositoryException, GraphManagerException {
        LinkedList<Repository> result = new LinkedList<>();
        new DownstreamWalker(this.graphManager, (parent, downstream, cycleInduced) -> {
            if(! cycleInduced) {
                if(parent.id().equals(repositoryId)) {
                    result.add(downstream);
                } else if(result.contains(downstream)) {
                    // downstream is downstream of a downstream, it is not a first level downstream
                    result.remove(downstream);
                }
            }
        }).startFrom(repositoryId);
        return result.toArray(new Repository[0]);
    }
}
