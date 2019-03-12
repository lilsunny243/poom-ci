package org.codingmatters.poom.ci.gremlin;

import io.flexio.docker.DockerResource;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class GremlinExploreTest {

    @ClassRule
    static public final DockerResource docker = GremlinResource.withGremlinContainer(DockerResource.client());

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Rule
    public GremlinResource gremlin = new GremlinResource(docker);

    @Test
    public void bla() throws Exception {
        TestGraph.setup(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()));

//        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection());
//        File out = this.tmp.newFile();
//        g.io(out.getAbsolutePath()).write().iterate();
//
//        System.out.println("#################################################################################");
//        try(Reader reader = new FileReader(out)) {
//            char[] buffer = new char[1024];
//            for(int read = reader.read(buffer) ; read != -1 ; read = reader.read(buffer)) {
//                System.out.printf("%s", new String(buffer, 0, read));
//            }
//        }
//        System.out.println("#################################################################################");


//        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection());
//        Vertex repo1 = g.V().hasLabel("repository").has("repository-id", "orga1-repo1-branch1").next();
//        System.out.printf("repo1 : %s", repo1);


//        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection());
//
//        System.out.printf("%s downstreams :: %s\n",
//                "orga-repo1-branch",
//                g.V().hasLabel("repository").has("repository-id", "orga-repo1-branch")
//                        .out("produces").hasLabel("module").in("depends-on").hasLabel("repository").toList());
//        System.out.printf("%s downstreams :: %s\n",
//                "orga-repo5-branch",
//                g.V().hasLabel("repository").has("repository-id", "orga-repo5-branch")
//                        .out("produces").hasLabel("module").in("depends-on").hasLabel("repository").toList());
    }
}
