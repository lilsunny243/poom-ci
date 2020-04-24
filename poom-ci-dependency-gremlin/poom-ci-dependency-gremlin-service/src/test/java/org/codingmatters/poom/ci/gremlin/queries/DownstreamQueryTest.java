package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;

public class DownstreamQueryTest extends AbstractVertexQueryTest {

    private DownstreamQuery<String> downstreams;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection());
        this.downstreams = new DownstreamQuery<>(g, map -> (String) map.get("repository-id").get(0).value());
    }

    @Test
    public void givenRepoProducesOneModule__whenNoRepoDependsOnThisModule__thenRepoADownstreamIsEmpty() throws Exception {
        assertThat(
                downstreams.forRepository("orga-repo4-branch"),
                is(empty())
        );
    }

    @Test
    public void givenRepoProducesNothin__thenRepoADownstreamIsEmpty() throws Exception {
        assertThat(
                downstreams.forRepository("orga-repo3-branch"),
                is(empty())
        );
    }

    @Test
    public void givenRepoAProducesOneModule__whenRepoBAndCDependsOnThisModule__thenRepoBAndCAreDownstreamOfA() throws Exception {
        assertThat(
                downstreams.forRepository("orga-repo2-branch"),
                containsInAnyOrder("orga-repo3-branch", "orga-repo4-branch")
        );
    }

    @Test
    public void givenRepoAProducesOneModule__whenRepoBDependsOnThisModule__thenRepoBIsDownstreamOfA() throws Exception {
        assertThat(
                downstreams.forRepository("orga-repo5-branch"),
                containsInAnyOrder("orga-repo3-branch")
        );
    }

    @Test
    public void givenRepoAProducesTwoModule__whenRepoBDependsOnOne_andRepoCAndDDependsOnTheOther__thenRepoBCAndDAreDownstreamOfA() throws Exception {
        assertThat(
                downstreams.forRepository("orga-repo1-branch"),
                containsInAnyOrder("orga-repo2-branch", "orga-repo4-branch")
        );
    }

}