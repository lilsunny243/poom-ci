package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class NextDownstreamsQueryTest extends AbstractVertexQueryTest {


    private NextDownstreamsQuery<String> nextDownstreams;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection());
        this.nextDownstreams = new NextDownstreamsQuery<>(g, map -> (String) map.get("repository-id").get(0).value());
    }

    @Test
    public void givenRepoDoesntHaveDownstreams__thenNextDownstreamsIsEmpty() throws Exception {
        assertThat(
                nextDownstreams.forRepository("orga-repo3-branch"),
                is(empty())
        );
        assertThat(
                nextDownstreams.forRepository("orga-repo4-branch"),
                is(empty())
        );
    }

    @Test
    public void givenRepoHasOneDownstream__thenThisDownstreamIsNext() throws Exception {
        assertThat(
                nextDownstreams.forRepository("orga-repo5-branch"),
                containsInAnyOrder("orga-repo3-branch")
        );
    }

    @Test
    public void givenRepoHaveTwoDownstream__whenThesAreNotDownstreamOfThisRepoDownstream__thenBothAreNext() throws Exception {
        assertThat(
                nextDownstreams.forRepository("orga-repo2-branch"),
                containsInAnyOrder("orga-repo3-branch", "orga-repo4-branch")
        );
    }

    @Test
    public void givenRepoBAndCAreDownstreams__whenRepoCIsDownstreamOfB__thenNextDownstreamIsOnlyB() throws Exception {
        assertThat(
                nextDownstreams.forRepository("orga-repo1-branch"),
                containsInAnyOrder("orga-repo2-branch")
        );
    }

    @Test
    public void givenCisDownstreamOfBandDisDownstreamOfC__whenBandDareDownstreamOfA__thenNextDownstreamOfAIsB() throws Exception {
        assertThat(
                nextDownstreams.forRepository("orga-repo10-branch"),
                containsInAnyOrder("orga-repo11-branch")
        );
    }
}