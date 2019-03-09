package org.codingmatters.poom.ci.gremlin;

import io.flexio.docker.DockerResource;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnectionException;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class GremlinExploreTest {


    @ClassRule
    static public final DockerResource docker = GremlinResource.withGremlinContainer(DockerResource.client());

    @Rule
    public GremlinResource gremlin = new GremlinResource(docker);

    @Test
    public void bla() throws Exception {
        TestGraph.setup(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()));

//        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection());
//        Vertex repo1 = g.V().hasLabel("repository").has("repository-id", "orga1-repo1-branch1").next();
//        System.out.printf("repo1 : %s", repo1);


        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection());

        System.out.printf("%s downstreams :: %s\n",
                "orga-repo1-branch",
                g.V().hasLabel("repository").has("repository-id", "orga-repo1-branch")
                        .out("produces").hasLabel("module").in("depends-on").hasLabel("repository").toList());
        System.out.printf("%s downstreams :: %s\n",
                "orga-repo5-branch",
                g.V().hasLabel("repository").has("repository-id", "orga-repo5-branch")
                        .out("produces").hasLabel("module").in("depends-on").hasLabel("repository").toList());
    }
}
