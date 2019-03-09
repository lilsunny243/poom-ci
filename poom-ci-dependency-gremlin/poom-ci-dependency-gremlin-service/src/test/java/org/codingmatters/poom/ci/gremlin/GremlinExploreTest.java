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
        this.createRepositoryGraph();

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

    private void createRepositoryGraph() throws Exception {
        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection());
        Vertex repo1 = g.addV("repository")
                .property("repository-id", "orga-repo1-branch")
                .property("name", "orga/repo1")
                .property("checkout-spec", "git|git@github.com:orga/repo1.git|branch")
                .next();
        Vertex repo2 = g.addV("repository")
                .property("repository-id", "orga-repo2-branch")
                .property("name", "orga/repo2")
                .property("checkout-spec", "git|git@github.com:orga/repo2.git|branch")
                .next();
        Vertex repo3 = g.addV("repository")
                .property("repository-id", "orga-repo3-branch")
                .property("name", "orga/repo3")
                .property("checkout-spec", "git|git@github.com:orga/repo3.git|branch")
                .next();
        Vertex repo4 = g.addV("repository")
                .property("repository-id", "orga-repo4-branch")
                .property("name", "orga/repo4")
                .property("checkout-spec", "git|git@github.com:orga/repo4.git|branch")
                .next();
        Vertex repo5 = g.addV("repository")
                .property("repository-id", "orga-repo5-branch")
                .property("name", "orga/repo5")
                .property("checkout-spec", "git|git@github.com:orga/repo5.git|branch")
                .next();

        Vertex module1 = g.addV("module")
                .property("spec", "group:module1")
                .property("version", "1")
                .next();
        Vertex module2 = g.addV("module")
                .property("spec", "group:module2")
                .property("version", "1")
                .next();
        Vertex module3 = g.addV("module")
                .property("spec", "group:module3")
                .property("version", "1")
                .next();
        Vertex module4 = g.addV("module")
                .property("spec", "group:module4")
                .property("version", "1")
                .next();
        Vertex externalModule = g.addV("module")
                .property("spec", "external:dep")
                .property("version", "1")
                .next();

        g.V(repo1).addE("produces").to(module1).next();
        g.V(repo1).addE("produces").to(module2).next();
        g.V(repo2).addE("produces").to(module3).next();
        g.V(repo5).addE("produces").to(module4).next();

        g.V(repo1).addE("depends-on").to(externalModule).next();
        g.V(repo2).addE("depends-on").to(module2).next();
        g.V(repo3).addE("depends-on").to(module3).next();
        g.V(repo3).addE("depends-on").to(module4).next();
        g.V(repo4).addE("depends-on").to(module2).next();
        g.V(repo4).addE("depends-on").to(module3).next();
    }

}
