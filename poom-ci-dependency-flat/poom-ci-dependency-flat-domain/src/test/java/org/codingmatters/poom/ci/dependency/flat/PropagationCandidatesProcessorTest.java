package org.codingmatters.poom.ci.dependency.flat;

import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.api.types.ValueList;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.DependsOnRelation;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.ProducesRelation;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class PropagationCandidatesProcessorTest {


    public static final FullRepository A_REPO = FullRepository.builder().id("a-repo").name("ARepo").checkoutSpec("a/repo").build();
    public static final FullRepository B_REPO = FullRepository.builder().id("b-repo").name("BRepo").checkoutSpec("b/repo").build();

    private static final Module V1_MODULE = Module.builder().spec("a").version("1").build();
    private static final Module V2_MODULE = Module.builder().spec("a").version("2").build();

    private final GraphManager graphManager = new GraphManager(
            InMemoryRepositoryWithPropertyQuery.validating(Repository.class),
            InMemoryRepositoryWithPropertyQuery.validating(ProducesRelation.class),
            InMemoryRepositoryWithPropertyQuery.validating(DependsOnRelation.class)
    );

    private final PropagationCandidatesProcessor processor = new PropagationCandidatesProcessor(this.graphManager);

    @Test
    public void givenRepositoryProducesAModule__whenARepositoryDependsOnAnotherVersion__thenARepositoryIsAPropagationCandidate() throws Exception {
        this.graphManager.index(A_REPO.withProduces(new ValueList.Builder().with(V2_MODULE).build()));
        this.graphManager.index(B_REPO.withDependencies(new ValueList.Builder().with(V1_MODULE).build()));

        assertThat(this.processor.candidates(A_REPO.id()), is(arrayContaining(this.from(B_REPO))));
    }

    @Test
    public void givenRepositoryProducesAModule__whenARepositoryDependsOnSameVersion__thenARepositoryIsNotAPropagationCandidate() throws Exception {
        this.graphManager.index(A_REPO.withProduces(new ValueList.Builder().with(V2_MODULE).build()));
        this.graphManager.index(B_REPO.withDependencies(new ValueList.Builder().with(V2_MODULE).build()));

        assertThat(this.processor.candidates(A_REPO.id()), is(not(arrayContaining(this.from(B_REPO)))));
    }




    private Repository from(FullRepository fullRepository) {
        return Repository.builder().id(fullRepository.id()).name(fullRepository.name()).checkoutSpec(fullRepository.checkoutSpec()).build();
    }
}