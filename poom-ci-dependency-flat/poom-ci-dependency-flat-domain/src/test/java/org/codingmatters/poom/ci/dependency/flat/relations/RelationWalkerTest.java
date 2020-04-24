package org.codingmatters.poom.ci.dependency.flat.relations;

import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;
import org.codingmatters.poom.ci.dependency.flat.GraphManagerException;
import org.codingmatters.poom.ci.dependency.flat.NoSuchRepositoryException;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.DependsOnRelation;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.ProducesRelation;
import org.codingmatters.poom.ci.dependency.flat.downstream.DownstreamWalker;
import org.codingmatters.poom.ci.dependency.flat.downstream.DownstreamWalkerListener;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;

public class RelationWalkerTest {

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

    private final List<String> walk = new LinkedList<>();
    private RelationWalkerListener walkerListener = (upstream, through, downstream) ->
            walk.add(String.format("%s => %s:%s => %s", upstream.id(), through.spec(), through.version(), downstream.id()))
            ;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void whenRepositoryNotIndexed__thenThrowsNoSuchRepositoryException() throws Exception {
        thrown.expect(NoSuchRepositoryException.class);
        new RelationWalker(this.graphManager, walkerListener).startFrom(A_REPO.id());
    }

    @Test
    public void givenRepositoryIndexed__whenRepositoryHasNoProduction__thenWalkIsEmpty() throws Exception {
        this.populateGraph(A_REPO, null, null);
        new RelationWalker(this.graphManager, walkerListener).startFrom(A_REPO.id());

        assertThat(walk, is(empty()));
    }

    @Test
    public void givenSomeRepositoryIndexed__whenRepositoryDirectRelation__thenWalksThroughOneRelation() throws Exception {
        this.populateGraph(A_REPO, null, new Module[] {A_MODULE});
        this.populateGraph(B_REPO, new Module[] {A_MODULE}, new Module[]{B_MODULE});

        new RelationWalker(this.graphManager, walkerListener).startFrom(A_REPO.id());

        assertThat(walk, contains("a-repo => a:1 => b-repo"));
    }

    @Test
    public void givenSomeRepositoryIndexed__whenRepositoryHasManyDirectRelation__thenWalksThroughTwoParallelRelations() throws Exception {
        this.populateGraph(A_REPO, null, new Module[] {A_MODULE});
        this.populateGraph(B_REPO, new Module[] {A_MODULE}, new Module[]{B_MODULE});
        this.populateGraph(C_REPO, new Module[] {A_MODULE}, null);

        new RelationWalker(this.graphManager, walkerListener).startFrom(A_REPO.id());

        assertThat(walk, contains("a-repo => a:1 => b-repo", "a-repo => a:1 => c-repo"));
    }

    @Test
    public void givenSomeRepositoryIndexed__whenRepositoryTransitiveRelations__thenWalksThroughTwoTransitive() throws Exception {
        this.populateGraph(A_REPO, null, new Module[] {A_MODULE});
        this.populateGraph(B_REPO, new Module[] {A_MODULE}, new Module[]{B_MODULE});
        this.populateGraph(C_REPO, new Module[] {B_MODULE}, null);

        new RelationWalker(this.graphManager, walkerListener).startFrom(A_REPO.id());

        assertThat(walk, contains("a-repo => a:1 => b-repo", "b-repo => b:1 => c-repo"));
    }

    @Test
    public void givenSomeRepositoryIndexed__whenTransitiveRelationsHasACycle__thenWalksThroughOnceAndCycleIsAlerted() throws Exception {
        this.populateGraph(A_REPO, new Module[] {C_MODULE}, new Module[]{A_MODULE});
        this.populateGraph(B_REPO, new Module[] {A_MODULE}, new Module[]{B_MODULE});
        this.populateGraph(C_REPO, new Module[] {B_MODULE}, new Module[]{C_MODULE});

        new RelationWalker(this.graphManager, walkerListener).startFrom(A_REPO.id());

        assertThat(walk, contains("a-repo => a:1 => b-repo", "b-repo => b:1 => c-repo", "c-repo => c:1 => a-repo"));
    }

    private void populateGraph(Repository repository, Module[] dependencies, Module[] produced) throws GraphManagerException {
        this.graphManager.index(FullRepository.builder()
                .id(repository.id()).checkoutSpec(repository.checkoutSpec()).name(repository.name())
                .dependencies(dependencies != null ? dependencies : new Module[0])
                .produces(produced != null ? produced : new Module[0])
                .build());
    }
}
