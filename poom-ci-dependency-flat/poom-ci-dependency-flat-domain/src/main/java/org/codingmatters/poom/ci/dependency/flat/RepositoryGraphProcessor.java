package org.codingmatters.poom.ci.dependency.flat;

import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.api.types.RepositoryGraph;
import org.codingmatters.poom.ci.dependency.api.types.RepositoryRelation;
import org.codingmatters.poom.ci.dependency.flat.downstream.DownstreamWalker;
import org.codingmatters.poom.ci.dependency.flat.relations.RelationWalker;

import java.util.*;

public class RepositoryGraphProcessor {
    private final GraphManager graphManager;

    public RepositoryGraphProcessor(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    public RepositoryGraph graph(String fromRepositoryId) throws GraphManagerException, NoSuchRepositoryException {
        Set<Repository> repositories = new HashSet<>();
        List<RepositoryRelation> relations = new LinkedList<>();

        Optional<Repository> root = this.graphManager.repository(fromRepositoryId);
        if(root.isPresent()) {
            repositories.add(root.get());
        } else {
            throw new NoSuchRepositoryException("no repo with id " + fromRepositoryId);
        }

        new RelationWalker(this.graphManager, (upstream, through, downstream) -> {
            System.out.printf("%s => %s => %s", upstream, through, downstream);
            repositories.add(upstream);
            repositories.add(downstream);
            relations.add(RepositoryRelation.builder()
                    .upstreamRepository(upstream.id())
                    .dependency(through)
                    .downstreamRepository(downstream.id())
                    .build());
        }).startFrom(fromRepositoryId);

        return RepositoryGraph.builder()
                .id(fromRepositoryId + "-graph")
                .roots(fromRepositoryId)
                .repositories(repositories)
                .relations(relations)
                .build();
    }
}
