package org.codingmatters.poom.ci.dependency.flat;

import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.api.types.ValueList;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.DependsOnRelation;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.ProducesRelation;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DownstreamProcessorTest {

    public static final FullRepository A_REPO = FullRepository.builder().id("a-repo").name("ARepo").checkoutSpec("a/repo").build();
    public static final FullRepository B_REPO = FullRepository.builder().id("b-repo").name("BRepo").checkoutSpec("b/repo").build();
    private static final Module A_MODULE = Module.builder().spec("a").version("1").build();
    private static final Module B_MODULE = Module.builder().spec("b").version("1").build();

    private GraphManager graphManager = new GraphManager(
            InMemoryRepositoryWithPropertyQuery.validating(Repository.class),
            InMemoryRepositoryWithPropertyQuery.validating(ProducesRelation.class),
            InMemoryRepositoryWithPropertyQuery.validating(DependsOnRelation.class)
    );

    private final DownstreamProcessor processor = new DownstreamProcessor(this.graphManager);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void whenGettingDownstreamOfUnexistentRepo__thenThrowsNoSuchRepositoryException() throws Exception {
        thrown.expect(NoSuchRepositoryException.class);
        this.processor.downstream("a-repo");
    }

    @Test
    public void givenRepositoryIndexed__whenRepositoryHasNoProduction__thenNoDownstream() throws Exception {
        this.graphManager.index(A_REPO);

        assertThat(this.processor.downstream(A_REPO.id()), is(emptyArray()));
    }

    @Test
    public void givenRepositoryIndexed__whenRepositoryHasProductions_andNoOtherRepoDependsOnIt__thenNoDownstream() throws Exception {
        this.graphManager.index(A_REPO.withProduces(new ValueList.Builder().with(A_MODULE).build()));
        this.graphManager.index(B_REPO.withDependencies(new ValueList.Builder().with(B_MODULE).build()));


        assertThat(this.processor.downstream(A_REPO.id()), is(emptyArray()));
    }

    @Test
    public void givenRepositoryIndexed__whenRepositoryHasProductions_andOtherRepoDependsOnIt__thenThisRepoIsADownstream() throws Exception {
        this.graphManager.index(A_REPO.withProduces(new ValueList.Builder().with(A_MODULE).build()));
        this.graphManager.index(B_REPO.withDependencies(new ValueList.Builder().with(A_MODULE).build()));

        assertThat(this.processor.downstream(A_REPO.id()), is(arrayContaining(this.from(B_REPO))));
    }

    private Repository from(FullRepository fullRepository) {
        return Repository.builder().id(fullRepository.id()).name(fullRepository.name()).checkoutSpec(fullRepository.checkoutSpec()).build();
    }
}