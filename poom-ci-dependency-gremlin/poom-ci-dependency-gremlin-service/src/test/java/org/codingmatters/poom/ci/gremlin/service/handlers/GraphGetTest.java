package org.codingmatters.poom.ci.gremlin.service.handlers;

import io.flexio.docker.DockerResource;
import org.codingmatters.poom.ci.dependency.api.RepositoryGraphGetRequest;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.api.types.RepositoryGraph;
import org.codingmatters.poom.ci.dependency.api.types.RepositoryRelation;
import org.codingmatters.poom.ci.gremlin.GremlinResource;
import org.codingmatters.poom.ci.gremlin.RealGraphLoader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class GraphGetTest {

    private final DockerResource docker = GremlinResource.withGremlinContainer(DockerResource.client());
    private final GremlinResource gremlin = new GremlinResource(docker);

    @Rule
    public TestRule chain= RuleChain
            .outerRule(docker)
            .around(gremlin);

    /**
     *
     * a:develop => a:a1:1 <-- b:develop
     *           => a:a2:1 <-- c:develop
     *
     * @throws Exception
     */
    @Test
    public void givenTreeDependencyGraph__whenGettingGraph__thenGraphReturned() throws Exception {
        RealGraphLoader.load("repo-graph-simple-tree", this.gremlin);

        RepositoryGraph actualGraph = new GraphGet(gremlin.remoteConnection()).apply(RepositoryGraphGetRequest.builder().build()).opt().status200().orElseThrow(() -> new AssertionError("failed getting graph")).payload();

        assertThat(actualGraph.repositories(), containsInAnyOrder(
                Repository.builder().id("org-repo-a-develop").name("org/repo-a.git").checkoutSpec("git|git@github.com:org/repo-a.git|develop").build(),
                Repository.builder().id("org-repo-b-develop").name("org/repo-b.git").checkoutSpec("git|git@github.com:org/repo-b.git|develop").build(),
                Repository.builder().id("org-repo-c-develop").name("org/repo-c.git").checkoutSpec("git|git@github.com:org/repo-c.git|develop").build()
        ));

        assertThat(actualGraph.relations(), containsInAnyOrder(
                RepositoryRelation.builder().upstreamRepository("org-repo-a-develop").dependency(Module.builder().spec("a:a1").version("1").build()).downstreamRepository("org-repo-b-develop").build(),
                RepositoryRelation.builder().upstreamRepository("org-repo-a-develop").dependency(Module.builder().spec("a:a2").version("1").build()).downstreamRepository("org-repo-c-develop").build()
        ));
        assertThat(actualGraph.roots(), containsInAnyOrder("org-repo-a-develop"));
    }
}