package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;

public class ProducedByQueryTest extends AbstractVertexQueryTest {

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


}