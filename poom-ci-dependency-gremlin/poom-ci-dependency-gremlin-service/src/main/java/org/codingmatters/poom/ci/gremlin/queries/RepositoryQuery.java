package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class RepositoryQuery<T> extends VertexQuery<T> {
    public RepositoryQuery(GraphTraversalSource graph, Function<Map<String, List<VertexProperty>>, T> vertexMapper) {
        super(graph, vertexMapper, "repository-id", "name", "checkout-spec");
    }

    public Optional<T> repository(String repositoryId) {
        List<T> result = this.processTraversal(this.graph().V().has("kind", "repository").has("repository-id", repositoryId));
        return Optional.ofNullable(result.isEmpty() ? null : result.get(0));
    }

    public List<T> all() {
        return this.processTraversal(this.graph().V().has("kind", "repository"));
    }
}
