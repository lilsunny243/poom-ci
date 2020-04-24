package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryPutRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryPutResponse;
import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class RepositoryCreateOrUpdateTest extends AbstractGraphManagerTest {

    private final RepositoryCreateOrUpdate handler = new RepositoryCreateOrUpdate(this.graphManager());

    @Test
    public void whenUpdatingWithoutRepositoryId__then400() throws Exception {
        RepositoryPutResponse response = this.handler.apply(RepositoryPutRequest.builder()
                .payload(FullRepository.builder().id("a-repo").name("ARepo").checkoutSpec("a/repo").build())
                .build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));
    }

    @Test
    public void whenUpdatingWithoutRepositorySpec__then400() throws Exception {
        RepositoryPutResponse response = this.handler.apply(RepositoryPutRequest.builder()
                .repositoryId("a-repo")
                .build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));
    }

    @Test
    public void givenRepositoryDoesntExist__whenUpdating__then200_andRepositoryIsCreated() throws Exception {
        RepositoryPutResponse response = this.handler.apply(RepositoryPutRequest.builder()
                .repositoryId("a-repo")
                .payload(FullRepository.builder().id("a-repo").name("ARepo").checkoutSpec("a/repo").build())
                .build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().payload(), is(Repository.builder().id("a-repo").name("ARepo").checkoutSpec("a/repo").build()));
        assertThat(this.graphManager().repository("a-repo").get(), is(Repository.builder().id("a-repo").name("ARepo").checkoutSpec("a/repo").build()));
    }

    @Test
    public void givenRepositoryDoesntExist__whenUpdating__then200_andRepositoryIsUpdated() throws Exception {
        this.graphManager().index(FullRepository.builder().id("a-repo").name("ARepo").checkoutSpec("a/repo").build());
        RepositoryPutResponse response = this.handler.apply(RepositoryPutRequest.builder()
                .repositoryId("a-repo")
                .payload(FullRepository.builder().id("a-repo").name("ARepoChanged").checkoutSpec("a/repo").build())
                .build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().payload(), is(Repository.builder().id("a-repo").name("ARepoChanged").checkoutSpec("a/repo").build()));
        assertThat(this.graphManager().repository("a-repo").get(), is(Repository.builder().id("a-repo").name("ARepoChanged").checkoutSpec("a/repo").build()));
    }
}