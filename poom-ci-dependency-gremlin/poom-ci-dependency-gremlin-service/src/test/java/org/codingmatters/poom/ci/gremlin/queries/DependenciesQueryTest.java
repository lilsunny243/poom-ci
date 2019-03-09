package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;

public class DependenciesQueryTest extends AbstractVertexQueryTest {

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
}