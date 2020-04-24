package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryDownstreamRepositoriesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryDownstreamRepositoriesGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;


public class RepositoryDownstreamsTest extends AbstractGraphManagerTest {

    private final RepositoryDownstreams handler = new RepositoryDownstreams(this.graphManager());

    @Test
    public void whenNotProvidingRepositoryId__then400() throws Exception {
        RepositoryDownstreamRepositoriesGetResponse response = this.handler.apply(RepositoryDownstreamRepositoriesGetRequest.builder().build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));
    }

    @Test
    public void whenRepositoryDoesntExist__then404() throws Exception {
        RepositoryDownstreamRepositoriesGetResponse response = this.handler.apply(RepositoryDownstreamRepositoriesGetRequest.builder()
                .repositoryId("no-such-repo")
                .build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));
    }

    @Test
    public void whenRepositoryExists__then200() throws Exception {
        this.graphManager().index(FullRepository.builder().id("a-repo").name("ARepo").checkoutSpec("a/repo").build());
        RepositoryDownstreamRepositoriesGetResponse response = this.handler.apply(RepositoryDownstreamRepositoriesGetRequest.builder()
                .repositoryId("a-repo")
                .build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().payload().toArray(), is(emptyArray()));
    }
}