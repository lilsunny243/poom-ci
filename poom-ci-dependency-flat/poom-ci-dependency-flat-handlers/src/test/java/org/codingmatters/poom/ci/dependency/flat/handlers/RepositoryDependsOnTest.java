package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryDependenciesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryDependenciesGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RepositoryDependsOnTest extends AbstractGraphManagerTest {

    private final RepositoryDependsOn handler = new RepositoryDependsOn(this.graphManager());

    @Test
    public void whenGettingDependenciesOfUnexistentEepo__then404() throws Exception {
        RepositoryDependenciesGetResponse response = this.handler.apply(RepositoryDependenciesGetRequest.builder().repositoryId("no-such-repo").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));
    }

    @Test
    public void givenRepoExists__whenGettingDependencies_andRepoHasNoDependencies__then200_andPayloadEmpty() throws Exception {
        this.graphManager().index(FullRepository.builder().id("a-repo").name("ARepo").checkoutSpec("a/repo").build());

        RepositoryDependenciesGetResponse response = this.handler.apply(RepositoryDependenciesGetRequest.builder().repositoryId("a-repo").build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().payload().toArray(), is(emptyArray()));
    }

    @Test
    public void givenRepoExists__whenGettingDependencies_andRepoSomeDependencies__then200_andPayloadEmpty() throws Exception {
        this.graphManager().index(FullRepository.builder().id("a-repo").name("ARepo").checkoutSpec("a/repo")
                .dependencies(
                        Module.builder().spec("m1").version("1").build(),
                        Module.builder().spec("m2").version("1").build()
                )
                .build());

        RepositoryDependenciesGetResponse response = this.handler.apply(RepositoryDependenciesGetRequest.builder().repositoryId("a-repo").build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().payload().toArray(), is(arrayContaining(
                Module.builder().spec("m1").version("1").build(),
                Module.builder().spec("m2").version("1").build()
        )));
    }
}