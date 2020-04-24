package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryDeleteRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryDeleteResponse;
import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;


public class RepositoryDeleteTest extends AbstractGraphManagerTest {

    private final RepositoryDelete handler = new RepositoryDelete(this.graphManager());

    @Test
    public void whenDeletingUnexistentRepository__then404() throws Exception {
        RepositoryDeleteResponse response = this.handler.apply(RepositoryDeleteRequest.builder().repositoryId("no-such-repo").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));
    }

    @Test
    public void givenRepositoryPopulated__whenDeletingExistingRepository__then200_andRepositoryRemoved() throws Exception {
        this.graphManager().index(FullRepository.builder().id("a-repo").checkoutSpec("a/repo").name("ARepo").build());

        RepositoryDeleteResponse response = this.handler.apply(RepositoryDeleteRequest.builder().repositoryId("a-repo").build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(this.graphManager().repositories(), is(emptyArray()));
    }
}