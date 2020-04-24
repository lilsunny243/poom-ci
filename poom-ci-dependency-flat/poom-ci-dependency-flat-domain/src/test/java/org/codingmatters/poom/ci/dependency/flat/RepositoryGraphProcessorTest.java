package org.codingmatters.poom.ci.dependency.flat;

import org.codingmatters.poom.ci.dependency.api.types.*;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.DependsOnRelation;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.ProducesRelation;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class RepositoryGraphProcessorTest {

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

    private final RepositoryGraphProcessor processor = new RepositoryGraphProcessor(graphManager);

    @Test
    public void whenRepositoryNotIndexed__thenThrowsNoSuchRepositoryException() throws Exception {
        thrown.expect(NoSuchRepositoryException.class);
        this.processor.graph(A_REPO.id());
    }

    @Test
    public void givenGraphPopulated__whenRepoHasNoDownstream__thenGraphHasNoRelation_andOneRepository_andOneRoot() throws Exception {
        this.populateGraph(A_REPO, new Module[]{}, new Module[]{});
        this.populateGraph(B_REPO, new Module[]{A_MODULE}, new Module[]{});

        assertThat(this.processor.graph(A_REPO.id()), is(RepositoryGraph.builder()
                .id(A_REPO.id() + "-graph")
                .roots(A_REPO.id())
                .repositories(A_REPO)
                .relations(Collections.emptyList())
                .build()));
    }

    @Test
    public void givenGraphPopulated__whenRepoHasDownstream__thenGraphHasRelations_andRelatedRepositoriesRepository_andOneRoot() throws Exception {
        this.populateGraph(A_REPO, new Module[]{}, new Module[]{A_MODULE});
        this.populateGraph(B_REPO, new Module[]{A_MODULE}, new Module[]{});

        RepositoryGraph actual = this.processor.graph(A_REPO.id());

        assertThat(actual.id(), is(A_REPO.id() + "-graph"));
        assertThat(actual.roots(), contains(A_REPO.id()));
        assertThat(actual.repositories(), containsInAnyOrder(A_REPO, B_REPO));
        assertThat(actual.relations(), containsInAnyOrder(RepositoryRelation.builder().upstreamRepository(A_REPO.id()).dependency(A_MODULE).downstreamRepository(B_REPO.id()).build()));
    }

    private void populateGraph(Repository repository, Module[] dependencies, Module[] produced) throws GraphManagerException {
        this.graphManager.index(FullRepository.builder()
                .id(repository.id()).checkoutSpec(repository.checkoutSpec()).name(repository.name())
                .dependencies(dependencies != null ? dependencies : new Module[0])
                .produces(produced != null ? produced : new Module[0])
                .build());
    }
}