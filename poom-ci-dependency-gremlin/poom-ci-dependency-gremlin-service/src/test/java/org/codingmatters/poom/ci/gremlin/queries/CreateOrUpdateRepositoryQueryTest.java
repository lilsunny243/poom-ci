package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CreateOrUpdateRepositoryQueryTest extends AbstractVertexQueryTest {

    @Test
    public void givenRepoDoesntExist__whenUpdatingById__thenRepoIsCreatedWithNameAndCheckoutSpec() throws Exception {
        String repositoryId = "orga-repo100-branch";
        String name = "orga/repo100";
        String checkoutSpec = "git|git@github.com:orga/repo2.git|branch";

        new CreateOrUpdateRepositoryQuery(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()))
                .update(repositoryId, name, checkoutSpec);

        Map<String, List<VertexProperty>> created = new RepositoryQuery<>(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()), map -> map)
                .repository(repositoryId)
                .get();

        assertThat(created.get("repository-id").get(0).value(), is(repositoryId));
        assertThat(created.get("name").get(0).value(), is(name));
        assertThat(created.get("checkout-spec").get(0).value(), is(checkoutSpec));
    }

    @Test
    public void givenRepoExists__whenUpdatingById__thenRepoIsUpdatedWithNameAndCheckoutSpec() throws Exception {
        String repositoryId = "orga-repo1-branch";
        String name = "orga/changed";
        String checkoutSpec = "git|git@github.com:orga/changed.git|branch";

        new CreateOrUpdateRepositoryQuery(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()))
                .update(repositoryId, name, checkoutSpec);

        Map<String, List<VertexProperty>> updated = new RepositoryQuery<>(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()), map -> map)
                .repository(repositoryId).get();

        assertThat(updated.get("repository-id").get(0).value(), is(repositoryId));
        assertThat(updated.get("name").get(0).value(), is(name));
        assertThat(updated.get("checkout-spec").get(0).value(), is(checkoutSpec));
    }
}