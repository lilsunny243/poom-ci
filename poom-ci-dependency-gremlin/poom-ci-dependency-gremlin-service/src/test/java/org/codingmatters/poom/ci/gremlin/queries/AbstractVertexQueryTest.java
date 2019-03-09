package org.codingmatters.poom.ci.gremlin.queries;

import io.flexio.docker.DockerResource;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.codingmatters.poom.ci.gremlin.GremlinResource;
import org.codingmatters.poom.ci.gremlin.TestGraph;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

public abstract class AbstractVertexQueryTest {

    @ClassRule
    static public final DockerResource docker = GremlinResource.withGremlinContainer(DockerResource.client());

    @Rule
    public GremlinResource gremlin = new GremlinResource(docker);

    @Before
    public void setUp() throws Exception {
        TestGraph.setup(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()));
    }

}
