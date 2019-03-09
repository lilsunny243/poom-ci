package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;

public class DownstreamQueryTest extends AbstractVertexQueryTest {

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


}