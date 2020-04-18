package org.codingmatters.poom.ci.dependency.flat;

import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.DependsOnRelation;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.ProducesRelation;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class GraphManagerTest {

    private static final org.codingmatters.poom.ci.dependency.api.types.Repository MY_REPO = org.codingmatters.poom.ci.dependency.api.types.Repository.builder()
            .id("my-repo-id")
            .name("my-repo")
            .checkoutSpec("my/repo/checkout/spec")
            .build();

    private static final Module MODULE_1 = Module.builder().spec("module-1").version("1.1").build();
    private static final Module MODULE_2 = Module.builder().spec("module-2").version("2.1").build();
    private static final int TEST_PAGE_SIZE = 10;


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Repository<org.codingmatters.poom.ci.dependency.api.types.Repository, PropertyQuery> repositories = InMemoryRepositoryWithPropertyQuery.validating(org.codingmatters.poom.ci.dependency.api.types.Repository.class);
    private Repository<ProducesRelation, PropertyQuery> producesRelations = InMemoryRepositoryWithPropertyQuery.validating(ProducesRelation.class);
    private Repository<DependsOnRelation, PropertyQuery> dependsOnRelations = InMemoryRepositoryWithPropertyQuery.validating(DependsOnRelation.class);

    private GraphManager manager = new GraphManager(repositories, this.producesRelations, this.dependsOnRelations, TEST_PAGE_SIZE);

    @Test
    public void givenReposAreEmpty__whenIndexing_andNoDependencies_andNoProduction__thenRepoIsStored_andNoRelationsAreStored() throws Exception {
        this.manager.index(FullRepository.builder()
                .id(MY_REPO.id())
                .name(MY_REPO.name())
                .checkoutSpec(MY_REPO.checkoutSpec())
                .build());

        assertThat(this.repositories.all(0, 1000).valueList(), contains(MY_REPO));
        assertThat(this.producesRelations.all(0, 1000).valueList(), is(empty()));
        assertThat(this.dependsOnRelations.all(0, 1000).valueList(), is(empty()));
    }

    @Test
    public void givenReposAreEmpty__whenIndexing_andHasDependencies_andHasProductions__thenRepoIsStored_andRelationsAreStored() throws Exception {
        this.manager.index(FullRepository.builder()
                .id(MY_REPO.id())
                .name(MY_REPO.name())
                .checkoutSpec(MY_REPO.checkoutSpec())
                .dependencies(MODULE_1)
                .produces(MODULE_2)
                .build());

        assertThat(this.repositories.all(0, 1000).valueList(), contains(MY_REPO));
        assertThat(this.dependsOnRelations.all(0, 1000).valueList(), contains(DependsOnRelation.builder().repository(MY_REPO).module(MODULE_1).build()));
        assertThat(this.producesRelations.all(0, 1000).valueList(), contains(ProducesRelation.builder().repository(MY_REPO).module(MODULE_2).build()));
    }

    @Test
    public void givenRepoAlreadyIndex__whenIndexingWithDifferentName__thenNameIsUpdated() throws Exception {
        this.repositories.createWithId(MY_REPO.id(), MY_REPO);

        this.manager.index(FullRepository.builder()
                .id(MY_REPO.id())
                .name("changed")
                .checkoutSpec(MY_REPO.checkoutSpec())
                .build());

        assertThat(this.repositories.all(0, 1000).valueList(), contains(MY_REPO.withName("changed")));
    }

    @Test
    public void givenRepoAlreadyIndex__whenIndexingWithDifferentSpec__thenSpecIsUpdated() throws Exception {
        this.repositories.createWithId(MY_REPO.id(), MY_REPO);

        this.manager.index(FullRepository.builder()
                .id(MY_REPO.id())
                .name(MY_REPO.name())
                .checkoutSpec("changed/spec")
                .build());

        assertThat(this.repositories.all(0, 1000).valueList(), contains(MY_REPO.withCheckoutSpec("changed/spec")));
    }

    @Test
    public void givenRepoAlreadyIndexed_andRepoHadRelations__whenIndexingWithNoRelations__thenRelationsAreWiped() throws Exception {
        this.repositories.createWithId(MY_REPO.id(), MY_REPO);
        this.dependsOnRelations.create(DependsOnRelation.builder().repository(MY_REPO).module(MODULE_1).build());
        this.producesRelations.create(ProducesRelation.builder().repository(MY_REPO).module(MODULE_2).build());

        this.manager.index(FullRepository.builder()
                .id(MY_REPO.id())
                .name(MY_REPO.name())
                .checkoutSpec(MY_REPO.checkoutSpec())
                .build());

        assertThat(this.repositories.all(0, 1000).valueList(), contains(MY_REPO));
        assertThat(this.producesRelations.all(0, 1000).valueList(), is(empty()));
        assertThat(this.dependsOnRelations.all(0, 1000).valueList(), is(empty()));
    }


    @Test
    public void givenARepoAlreadyIndex__whenIndexingARepoWithDifferenId__thenBothReposAreIndexed() throws Exception {
        this.repositories.createWithId(MY_REPO.id(), MY_REPO);

        this.manager.index(FullRepository.builder()
                .id("another-id")
                .name(MY_REPO.name())
                .checkoutSpec(MY_REPO.checkoutSpec())
                .build());

        assertThat(this.repositories.all(0, 1000).valueList(), contains(
                MY_REPO,
                MY_REPO.withId("another-id")
        ));
    }

    @Test
    public void givenARepoAlreadyIndex_andRepoHasRelations__whenIndexingARepoWithDifferentId__thenBothReposAreIndexedAndRelationsAreKept() throws Exception {
        this.repositories.createWithId(MY_REPO.id(), MY_REPO);
        this.dependsOnRelations.create(DependsOnRelation.builder().repository(MY_REPO).module(MODULE_1).build());
        this.producesRelations.create(ProducesRelation.builder().repository(MY_REPO).module(MODULE_2).build());

        this.manager.index(FullRepository.builder()
                .id("another-id")
                .name(MY_REPO.name())
                .checkoutSpec(MY_REPO.checkoutSpec())
                .build());

        assertThat(this.repositories.all(0, 1000).valueList(), contains(
                MY_REPO,
                MY_REPO.withId("another-id")
        ));
        assertThat(this.dependsOnRelations.all(0, 1000).valueList(), contains(DependsOnRelation.builder().repository(MY_REPO).module(MODULE_1).build()));
        assertThat(this.producesRelations.all(0, 1000).valueList(), contains(ProducesRelation.builder().repository(MY_REPO).module(MODULE_2).build()));
    }


    @Test
    public void givenRepoNotIndexed__whenGettingProductions__thenNoSuchRepositoryException() throws Exception {
        thrown.expect(GraphManagerException.class);
        thrown.expectMessage("no such repository");

        this.manager.producedBy(MY_REPO);
    }

    @Test
    public void givenRepoNotIndexed__whenGettingDependencies__thenNoSuchRepositoryException() throws Exception {
        thrown.expect(GraphManagerException.class);
        thrown.expectMessage("no such repository");

        this.manager.dependenciesOf(MY_REPO);
    }

    @Test
    public void givenRepoIndexed_andRepoHasOneProduction__whenGettingProducedBy__thenProductionReturned() throws Exception {
        this.repositories.createWithId(MY_REPO.id(), MY_REPO);
        this.dependsOnRelations.create(DependsOnRelation.builder().repository(MY_REPO).module(MODULE_1).build());
        this.producesRelations.create(ProducesRelation.builder().repository(MY_REPO).module(MODULE_2).build());

        assertThat(this.manager.producedBy(MY_REPO), is(arrayContaining(MODULE_2)));
    }

    @Test
    public void givenRepoIndexed_andRepoHasOneDependency__whenGettingDependencies__thenDependencyReturned() throws Exception {
        this.repositories.createWithId(MY_REPO.id(), MY_REPO);
        this.dependsOnRelations.create(DependsOnRelation.builder().repository(MY_REPO).module(MODULE_1).build());
        this.producesRelations.create(ProducesRelation.builder().repository(MY_REPO).module(MODULE_2).build());

        assertThat(this.manager.dependenciesOf(MY_REPO), is(arrayContaining(MODULE_1)));
    }

    @Test
    public void paging() throws Exception {
        int count = 5 * TEST_PAGE_SIZE + TEST_PAGE_SIZE / 3;

        this.repositories.createWithId(MY_REPO.id(), MY_REPO);
        for (int i = 0; i < count; i++) {
            this.dependsOnRelations.create(DependsOnRelation.builder().repository(MY_REPO).module(MODULE_1.withSpec("module-dep-" + i)).build());
            this.producesRelations.create(ProducesRelation.builder().repository(MY_REPO).module(MODULE_2.withSpec("module-prod-" + i)).build());
        }

        assertThat(this.manager.dependenciesOf(MY_REPO), arrayWithSize(count));
        assertThat(this.manager.producedBy(MY_REPO), arrayWithSize(count));
    }
}