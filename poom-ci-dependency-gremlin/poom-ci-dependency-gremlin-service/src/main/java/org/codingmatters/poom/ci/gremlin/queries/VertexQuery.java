package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VertexQuery<T> {
    private final GraphTraversalSource graph;
    private final Function<Map<String, List<VertexProperty>>, T> vertexMapper;
    private String[] propertyNames;

    public VertexQuery(GraphTraversalSource graph, Function<Map<String, List<VertexProperty>>, T> vertexMapper, String ... propertyNames) {
        this.graph = graph;
        this.vertexMapper = vertexMapper;
        this.propertyNames = propertyNames;
    }

    protected GraphTraversalSource graph() {
        return this.graph;
    }


    protected List<T> processTraversal(GraphTraversal<Vertex, Vertex> traversal) {
        List<Map<String, Object>> dependencies = traversal
                .propertyMap(this.propertyNames)
                .toList();

        return dependencies.stream()
                .map(map -> {
                    Map<String, List<VertexProperty>> casted = new HashMap<>();
                    map.forEach((s, o) -> casted.put(s, (List<VertexProperty>)o));
                    return this.vertexMapper.apply(casted);
                })
                .collect(Collectors.toList());
    }
}
