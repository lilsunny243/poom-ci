package org.codingmatters.poom.ci.dependency.flat;

import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.DependsOnRelation;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.ProducesRelation;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class FirstLevelDownstreamProcessorTest {

    public static final Repository A_REPO = Repository.builder().id("a-repo").name("ARepo").checkoutSpec("a/repo").build();
    public static final Repository B_REPO = Repository.builder().id("b-repo").name("BRepo").checkoutSpec("b/repo").build();
    public static final Repository C_REPO = Repository.builder().id("c-repo").name("CRepo").checkoutSpec("c/repo").build();
    private static final Module A_MODULE = Module.builder().spec("a").version("1").build();
    private static final Module B_MODULE = Module.builder().spec("b").version("1").build();
    private static final Module C_MODULE = Module.builder().spec("c").version("1").build();

    private GraphManager graphManager = new GraphManager(
            InMemoryRepositoryWithPropertyQuery.validating(Repository.class),
            InMemoryRepositoryWithPropertyQuery.validating(ProducesRelation.class),
            InMemoryRepositoryWithPropertyQuery.validating(DependsOnRelation.class)
    );

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final FirstLevelDownstreamProcessor processor = new FirstLevelDownstreamProcessor(this.graphManager);

    @Test
    public void whenRepositoryDoesntExists__thenThrowsNoSuchRepositoryException() throws Exception {
        thrown.expect(NoSuchRepositoryException.class);
        this.processor.downstream(A_REPO.id());
    }

    @Test
    public void givenRepoPopulated__whenNoDownstream__thenNoFirstLevelDownstream() throws Exception {
        this.populateGraph(A_REPO, new Module[]{}, new Module[]{});
        this.populateGraph(B_REPO, new Module[]{A_MODULE}, new Module[]{});

        assertThat(this.processor.downstream(A_REPO.id()), is(emptyArray()));
    }

    @Test
    public void givenRepoPopulated__whenDirectDownstream__thenFirstLevelDownstream() throws Exception {
        this.populateGraph(A_REPO, new Module[]{}, new Module[]{A_MODULE});
        this.populateGraph(B_REPO, new Module[]{A_MODULE}, new Module[]{});

        assertThat(this.processor.downstream(A_REPO.id()), is(arrayContaining(B_REPO)));
    }

    @Test
    public void givenRepoPopulated__whenTransitiveDownstream__thenTransitiveAreNotFirstLevelDownstream() throws Exception {
        this.populateGraph(A_REPO, new Module[]{}, new Module[]{A_MODULE});
        this.populateGraph(B_REPO, new Module[]{A_MODULE}, new Module[]{B_MODULE});
        this.populateGraph(C_REPO, new Module[]{B_MODULE}, new Module[]{});

        assertThat(this.processor.downstream(A_REPO.id()), is(arrayContaining(B_REPO)));
    }

    @Test
    public void givenRepoPopulated__whenADirectDownstreamIsDownstreamOfAFirstLevelDownstream__thenItsNotAFirstLevelDownstream() throws Exception {
        this.populateGraph(A_REPO, new Module[]{}, new Module[]{A_MODULE});
        this.populateGraph(B_REPO, new Module[]{A_MODULE}, new Module[]{B_MODULE});
        this.populateGraph(C_REPO, new Module[]{A_MODULE, B_MODULE}, new Module[]{});

        assertThat(this.processor.downstream(A_REPO.id()), is(arrayContaining(B_REPO)));
    }

    private void populateGraph(Repository repository, Module[] dependencies, Module[] produced) throws GraphManagerException {
        this.graphManager.index(FullRepository.builder()
                .id(repository.id()).checkoutSpec(repository.checkoutSpec()).name(repository.name())
                .dependencies(dependencies != null ? dependencies : new Module[0])
                .produces(produced != null ? produced : new Module[0])
                .build());
    }
}