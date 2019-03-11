package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.apache.tinkerpop.gremlin.process.traversal.P.within;

public class NextDownstreamsQuery<T> extends VertexQuery<T> {
    private final DownstreamQuery<String> downstreams;
    private final TransitiveDownstreamQuery<String> transitiveDownstreams;

    public NextDownstreamsQuery(GraphTraversalSource graph, Function<Map<String, List<VertexProperty>>, T> vertexMapper) {
        super(graph, vertexMapper, "repository-id", "name", "checkoutSpec");
        this.downstreams = new DownstreamQuery<>(graph, map -> (String) map.get("repository-id").get(0).value());
        this.transitiveDownstreams = new TransitiveDownstreamQuery<>(graph, map -> (String) map.get("repository-id").get(0).value());
    }

    public List<T> forRepository(String repositoryId) {
        Set<String> all = new HashSet<>();

        List<String> directRepositoryIds = this.downstreams.forRepository(repositoryId);
        all.addAll(directRepositoryIds);

        for (String directRepositoryId : directRepositoryIds) {
            List<String> transitiveRepositoryIds = this.transitiveDownstreams.forRepository(directRepositoryId);
            all.removeAll(transitiveRepositoryIds);
        }

        return this.processTraversal(
                this.graph().V().hasLabel("repository").has("repository-id", within(all.toArray()))
        );
    }
}
