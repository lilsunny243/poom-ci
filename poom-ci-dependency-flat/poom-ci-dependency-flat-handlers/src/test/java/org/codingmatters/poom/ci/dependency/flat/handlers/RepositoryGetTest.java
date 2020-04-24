package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class RepositoryGetTest extends AbstractGraphManagerTest {

    private final RepositoryGet handler = new RepositoryGet(this.graphManager());

    @Test
    public void whenGettingARepositoryWithoutRepositoryId__then400() throws Exception {
        RepositoryGetResponse response = this.handler.apply(RepositoryGetRequest.builder().build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));
    }

    @Test
    public void givenRepositoryEmpty__whenGettingARepository__then404() throws Exception {
        RepositoryGetResponse response = this.handler.apply(RepositoryGetRequest.builder().repositoryId("no-such-repo").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));
    }

    @Test
    public void givenRepositoryPopulated__whenGettingUnexistentRepo__then404() throws Exception {
        this.graphManager().index(FullRepository.builder().id("a-repo").name("ARepo").checkoutSpec("a/repo").build());

        RepositoryGetResponse response = this.handler.apply(RepositoryGetRequest.builder().repositoryId("no-such-repo").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));
    }

    @Test
    public void givenRepositoryPopulated__whenGettingExistingRepo__then200_andRepoReturned() throws Exception {
        this.graphManager().index(FullRepository.builder().id("a-repo").name("ARepo").checkoutSpec("a/repo").build());

        RepositoryGetResponse response = this.handler.apply(RepositoryGetRequest.builder().repositoryId("a-repo").build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().payload(), is(Repository.builder().id("a-repo").name("ARepo").checkoutSpec("a/repo").build()));
    }
}