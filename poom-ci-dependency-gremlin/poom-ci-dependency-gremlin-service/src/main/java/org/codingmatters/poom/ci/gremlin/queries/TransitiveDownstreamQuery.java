package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.apache.tinkerpop.gremlin.process.traversal.P.within;

public class TransitiveDownstreamQuery<T> extends VertexQuery<T> {
    private final DownstreamQuery<String> downstreams;

    public TransitiveDownstreamQuery(GraphTraversalSource graph, Function<Map<String, List<VertexProperty>>, T> vertexMapper) {
        super(graph, vertexMapper, "repository-id", "name", "checkoutSpec");
        this.downstreams = new DownstreamQuery<>(graph, map -> (String) map.get("repository-id").get(0).value());
    }

    public List<T> forRepository(String repositoryId) {
        Set<String> all = new HashSet<>();

        List<String> downstreamRepositoryIds = this.downstreams.forRepository(repositoryId);
        this.addTransitive(downstreamRepositoryIds, all);

        return this.processTraversal(
                this.graph().V().has("kind", "repository").has("repository-id", within(all.toArray()))
        );
    }

    private void addTransitive(List<String> repositoryIds, Set<String> to) {
        for (String repositoryId : repositoryIds) {
            if(! to.contains(repositoryId)) {
                this.addTransitive(this.downstreams.forRepository(repositoryId), to);
                to.add(repositoryId);
            }
        }
    }
}
