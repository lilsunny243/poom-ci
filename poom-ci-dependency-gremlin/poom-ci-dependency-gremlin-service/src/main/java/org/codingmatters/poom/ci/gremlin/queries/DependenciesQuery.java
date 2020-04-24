package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DependenciesQuery<T> extends VertexQuery<T> {

    public DependenciesQuery(GraphTraversalSource graph, Function<Map<String, List<VertexProperty>>, T> vertexMapper) {
        super(graph, vertexMapper,"spec", "version");
    }

    public List<T> forRepository(String repositoryId) {
        Vertex repo = this.graph().V()
                .has("kind", "repository")
                .has("repository-id", repositoryId).next();

        return this.processTraversal(
                this.graph().V(repo.id()).out("depends-on").has("kind", "module")
        );
    }
}
