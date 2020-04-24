package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryGraphGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryGraphGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.api.types.RepositoryGraph;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class RepositoryGraphGetTest extends AbstractGraphManagerTest {

    private final RepositoryGraphGet handler = new RepositoryGraphGet(this.graphManager());

    @Test
    public void whenRepositoryDoesntExist__then404() throws Exception {
        RepositoryGraphGetResponse response = this.handler.apply(RepositoryGraphGetRequest.builder().root("a-repo").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));
    }

    @Test
    public void whenNoRootRepositoryIdProvided__then400() throws Exception {
        RepositoryGraphGetResponse response = this.handler.apply(RepositoryGraphGetRequest.builder().build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));
    }

    @Test
    public void givenRepositoryExists__whenRepositoryIsRoot__then200_andRepositoryGraphReturned() throws Exception {
        this.graphManager().index(FullRepository.builder().id("a-repo").checkoutSpec("a/repo").name("ARepo").build());

        RepositoryGraphGetResponse response = this.handler.apply(RepositoryGraphGetRequest.builder().root("a-repo").build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));
        assertThat(response.status200().payload(), is(RepositoryGraph.builder()
                .id("a-repo-graph")
                .roots("a-repo")
                .repositories(Repository.builder().id("a-repo").checkoutSpec("a/repo").name("ARepo").build())
                .relations(Collections.emptyList()).build()));
    }
}