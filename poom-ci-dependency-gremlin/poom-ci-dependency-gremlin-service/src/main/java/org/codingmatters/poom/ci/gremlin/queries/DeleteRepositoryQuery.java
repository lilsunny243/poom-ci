package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class DeleteRepositoryQuery {

    private final GraphTraversalSource g;

    public DeleteRepositoryQuery(GraphTraversalSource g) {
        this.g = g;
    }

    public void delete(String repositoryId) {
        Vertex repo = this.g.V().hasLabel("repository").has("repository-id", repositoryId).next();

        new UpdateRepositoryDependenciesQuery(this.g).update(repositoryId);
        new UpdateRepositoryProducedByQuery(this.g).update(repositoryId);
        this.g.V(repo).drop().iterate();
    }
}
