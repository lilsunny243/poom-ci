package org.codingmatters.poom.ci.gremlin;

import io.flexio.docker.DockerResource;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.codingmatters.poom.ci.gremlin.queries.DependenciesQuery;
import org.codingmatters.poom.ci.gremlin.queries.DownstreamQuery;
import org.codingmatters.poom.ci.gremlin.queries.ProducedByQuery;
import org.junit.*;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class RepositoryGraphTest {

    @ClassRule
    static public final DockerResource docker = GremlinResource.withGremlinContainer(DockerResource.client());

    @Rule
    public GremlinResource gremlin = new GremlinResource(docker);

    @Before
    public void setUp() throws Exception {
        this.createRepositoryGraph();
    }

    @Test
    public void givenRepositoryExists__whenRequestingDependencies__thenAllModuleVertexConnectedDirectlyWithDependsOnLabelAreReturned() throws Exception {
        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection());
        DependenciesQuery<String> dependencyQuery = new DependenciesQuery<>(g, map -> (String) map.get("spec").get(0).value());

        assertThat(
                dependencyQuery.forRepository("orga-repo1-branch"),
                containsInAnyOrder("external:dep")
        );
        assertThat(
                dependencyQuery.forRepository("orga-repo2-branch"),
                containsInAnyOrder("group:module2")
        );
        assertThat(
                dependencyQuery.forRepository("orga-repo3-branch"),
                containsInAnyOrder("group:module3", "group:module4")
        );
        assertThat(
                dependencyQuery.forRepository("orga-repo4-branch"),
                containsInAnyOrder("group:module1", "group:module3")
        );
    }

    @Test
    public void givenRepositoryExists__whenRequestingProduced__thenAllModuleVertexConnectedDirectlyWithProducesLabelAreReturned() throws Exception {
        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection());
        ProducedByQuery<String> producedByQuery = new ProducedByQuery<>(g, map -> (String) map.get("spec").get(0).value());

        assertThat(
                producedByQuery.forRepository("orga-repo1-branch"),
                containsInAnyOrder("group:module1", "group:module2")
        );
        assertThat(
                producedByQuery.forRepository("orga-repo2-branch"),
                containsInAnyOrder("group:module3")
        );
        assertThat(
                producedByQuery.forRepository("orga-repo3-branch"),
                is(empty())
        );
        assertThat(
                producedByQuery.forRepository("orga-repo4-branch"),
                containsInAnyOrder("group:module5")
        );
        assertThat(
                producedByQuery.forRepository("orga-repo5-branch"),
                containsInAnyOrder("group:module4")
        );
    }

    @Test
    public void givenRepoProducesOneModule__whenNoRepoDependsOnThisModule__thenRepoADownstreamIsEmpty() throws Exception {
        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection());
        DownstreamQuery<String> producedByQuery = new DownstreamQuery<>(g, map -> (String) map.get("repository-id").get(0).value());

        assertThat(
                producedByQuery.forRepository("orga-repo4-branch"),
                is(empty())
        );
    }

    @Test
    public void givenRepoProducesNothin__thenRepoADownstreamIsEmpty() throws Exception {
        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection());
        DownstreamQuery<String> producedByQuery = new DownstreamQuery<>(g, map -> (String) map.get("repository-id").get(0).value());

        assertThat(
                producedByQuery.forRepository("orga-repo3-branch"),
                is(empty())
        );
    }

    @Test
    public void givenRepoAProducesOneModule__whenRepoBAndCDependsOnThisModule__thenRepoBAndCAreDownstreamOfA() throws Exception {
        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection());
        DownstreamQuery<String> producedByQuery = new DownstreamQuery<>(g, map -> (String) map.get("repository-id").get(0).value());

        assertThat(
                producedByQuery.forRepository("orga-repo2-branch"),
                containsInAnyOrder("orga-repo3-branch", "orga-repo4-branch")
        );
    }

    @Test
    public void givenRepoAProducesOneModule__whenRepoBDependsOnThisModule__thenRepoBIsDownstreamOfA() throws Exception {
        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection());
        DownstreamQuery<String> producedByQuery = new DownstreamQuery<>(g, map -> (String) map.get("repository-id").get(0).value());

        assertThat(
                producedByQuery.forRepository("orga-repo5-branch"),
                containsInAnyOrder("orga-repo3-branch")
        );
    }

    @Test
    public void givenRepoAProducesTwoModule__whenRepoBDependsOnOne_andRepoCAndDDependsOnTheOther__thenRepoBCAndDAreDownstreamOfA() throws Exception {
        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection());
        DownstreamQuery<String> producedByQuery = new DownstreamQuery<>(g, map -> (String) map.get("repository-id").get(0).value());

        assertThat(
                producedByQuery.forRepository("orga-repo1-branch"),
                containsInAnyOrder("orga-repo2-branch", "orga-repo4-branch")
        );
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
        Vertex module5 = g.addV("module")
                .property("spec", "group:module5")
                .property("version", "1")
                .next();
        Vertex externalModule = g.addV("module")
                .property("spec", "external:dep")
                .property("version", "1")
                .next();

        g.V(repo1).addE("produces").to(module1).next();
        g.V(repo1).addE("produces").to(module2).next();
        g.V(repo2).addE("produces").to(module3).next();
        g.V(repo4).addE("produces").to(module5).next();
        g.V(repo5).addE("produces").to(module4).next();

        g.V(repo1).addE("depends-on").to(externalModule).next();
        g.V(repo2).addE("depends-on").to(module2).next();
        g.V(repo3).addE("depends-on").to(module3).next();
        g.V(repo3).addE("depends-on").to(module4).next();
        g.V(repo4).addE("depends-on").to(module1).next();
        g.V(repo4).addE("depends-on").to(module3).next();
    }
}
