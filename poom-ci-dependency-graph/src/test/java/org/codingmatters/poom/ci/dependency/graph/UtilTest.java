package org.codingmatters.poom.ci.dependency.graph;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.apache.tinkerpop.gremlin.structure.io.IoCore.graphml;

public class UtilTest {

    @Test
    public void showGraph() throws Exception {
        Graph graph = TinkerGraph.open();
        this.load(graph, "real-graph.xml");

    }

    private void load(Graph graph, String resource) throws IOException, URISyntaxException {
        graph.io(graphml()).readGraph(new File(Thread.currentThread().getContextClassLoader().getResource(resource).toURI()).getAbsolutePath());
    }
}
