package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class TransitiveDownstreamQueryTest extends AbstractVertexQueryTest {

    private TransitiveDownstreamQuery<String> downstreams;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection());
        this.downstreams = new TransitiveDownstreamQuery<>(g, map -> (String) map.get("repository-id").get(0).value());
    }

    @Test
    public void givenRepoASOnlyDirectDownstream__thenTransitiveDownstreamIsDirectDownstream() throws Exception {
        assertThat(
                downstreams.forRepository("orga-repo5-branch"),
                containsInAnyOrder("orga-repo3-branch")
        );
    }

    @Test
    public void givenRepoDownstreamHasDownstreams__thenTransitiveDownstreamIsTheUnionOfAllDownstreams() throws Exception {
        assertThat(
                downstreams.forRepository("orga-repo1-branch"),
                containsInAnyOrder("orga-repo2-branch", "orga-repo3-branch", "orga-repo4-branch")
        );
    }
}