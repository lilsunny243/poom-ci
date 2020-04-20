package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoriesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoriesGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


public class RepositoryListTest extends AbstractGraphManagerTest {

    private final RepositoryList handler = new RepositoryList(this.graphManager());

    @Test
    public void givenNoRepositories__whenGettingRepositoryList__then200_andEmptyPayload() throws Exception {
        RepositoriesGetResponse response = this.handler.apply(RepositoriesGetRequest.builder().build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200 got " + response));

        assertThat(response.status200().payload().toArray(), is(emptyArray()));
    }

    @Test
    public void givenSomeRepositories__whenGettingRepositoryList__then200_andCompleteListReturned() throws Exception {
        this.graphManager()
                .index(FullRepository.builder().id("a").checkoutSpec("a/a").name("A").build())
                .index(FullRepository.builder().id("b").checkoutSpec("b/b").name("B").build())
                .index(FullRepository.builder().id("c").checkoutSpec("c/c").name("C").build())
        ;
        RepositoriesGetResponse response = this.handler.apply(RepositoriesGetRequest.builder().build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200 got " + response));

        assertThat(response.status200().payload().toArray(), is(arrayContaining(
                Repository.builder().id("a").checkoutSpec("a/a").name("A").build(),
                Repository.builder().id("b").checkoutSpec("b/b").name("B").build(),
                Repository.builder().id("c").checkoutSpec("c/c").name("C").build()
        )));
    }
}