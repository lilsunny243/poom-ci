package org.codingmatters.poom.ci.gremlin;

import io.flexio.docker.DockerResource;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.Test;

public class GremlinExploreTest {

    public DockerResource docker = DockerResource.client()
            .with("gremlin", container -> {
                return container.image("tinkerpop/gremlin-server:3.4.0");
            }).started().finallyStopped();

    @Test
    public void given__when__then() throws Exception {
        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote("conf/remote-graph.properties");
    }
}
