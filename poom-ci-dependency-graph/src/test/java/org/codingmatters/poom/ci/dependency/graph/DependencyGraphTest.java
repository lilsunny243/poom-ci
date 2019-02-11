package org.codingmatters.poom.ci.dependency.graph;

import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.graph.tinkerpop.TinkerPopDependencyGraph;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class DependencyGraphTest {

    @Test
    public void givenGraphIsEmpty__whenListingRepositoris__thenRepositoryListIsEmpty() throws Exception {
        assertThat(new TinkerPopDependencyGraph().repositories(), is(emptyArray()));
    }

    @Test
    public void createRepositories() throws Exception {
        int count = 23;
        Repository[] repos = new Repository[count];

        TinkerPopDependencyGraph graph = new TinkerPopDependencyGraph();
        for (int i = 0; i < count; i++) {
            Repository repository = Repository.builder()
                    .id("repo-" + i)
                    .name("repo-" + i)
                    .checkoutSpec("checkout/spec/" + i)
                    .build();
            repos[i] = repository;
            graph.add(repository);
        }

        assertThat(graph.repositories(), arrayContainingInAnyOrder(repos));
    }

    @Test
    public void repositoriesById() throws Exception {
        int count = 23;
        Repository[] repos = new Repository[count];

        TinkerPopDependencyGraph graph = new TinkerPopDependencyGraph();
        for (int i = 0; i < count; i++) {
            Repository repository = Repository.builder()
                    .id("repo-" + i)
                    .name("repo-" + i)
                    .checkoutSpec("checkout/spec/" + i)
                    .build();
            repos[i] = repository;
            graph.add(repository);
        }

        assertThat(graph.repositoryById("repo-12").get(), is(repos[12]));
    }

    @Test
    public void deleteRepository() throws Exception {
        TinkerPopDependencyGraph graph = new TinkerPopDependencyGraph();
        Repository repository = Repository.builder()
                .id("repo")
                .name("repo")
                .checkoutSpec("checkout/spec")
                .build();
        graph.add(repository);
        assertTrue(graph.repositoryById("repo").isPresent());

        graph.remove(repository);

        assertFalse(graph.repositoryById("repo").isPresent());
    }

    @Test
    public void deleteUnexistentRepositoryIsSilent() throws Exception {
        TinkerPopDependencyGraph graph = new TinkerPopDependencyGraph();
        Repository repository = Repository.builder()
                .id("repo")
                .name("repo")
                .checkoutSpec("checkout/spec")
                .build();
        graph.add(repository);
        graph.remove(repository);

        graph.remove(repository);
    }

    @Test
    public void givenRepositoryExists__whenReAddingRepo__thenRepositoryIsNotCreated__andRepositoryIsNotUpdated() throws Exception {
        int count = 23;

        TinkerPopDependencyGraph graph = new TinkerPopDependencyGraph();

        graph.add(Repository.builder()
                .id("repo")
                .name("repo")
                .checkoutSpec("checkout/spec")
                .build());
        for (int i = 0; i < count; i++) {
            Repository repository = Repository.builder()
                    .id("repo")
                    .name("repo-" + i)
                    .checkoutSpec("checkout/spec/" + i)
                    .build();
            graph.add(repository);
        }

        assertThat(graph.repositories(), arrayContainingInAnyOrder(Repository.builder()
                .id("repo")
                .name("repo")
                .checkoutSpec("checkout/spec")
                .build()));
    }

    @Test
    public void createModules_noMultipleCreation() throws Exception {
        int count = 23;

        DependencyGraph graph = new TinkerPopDependencyGraph();
        for (int i = 0; i < count; i++) {
            Module module = Module.builder()
                    .spec("module:spec")
                    .version("1-SNAPSHOT")
                    .build();
            graph.add(module);
        }

        assertThat(graph.modules(), arrayContainingInAnyOrder(Module.builder()
                .spec("module:spec")
                .version("1-SNAPSHOT")
                .build()));
    }

    @Test
    public void createModules() throws Exception {
        int count = 23;
        Module[] modules = new Module[count];

        DependencyGraph graph = new TinkerPopDependencyGraph();
        for (int i = 0; i < count; i++) {
            Module module = Module.builder()
                    .spec("module:" + i)
                    .version(i + "-SNAPSHOT")
                    .build();
            modules[i] = module;
            graph.add(module);
        }

        assertThat(graph.modules(), arrayContainingInAnyOrder(modules));
    }

    @Test
    public void createProduces() throws Exception {
        Repository repository = Repository.builder()
                .id("repo")
                .name("repo")
                .checkoutSpec("checkout/spec/0")
                .build();
        Module module = Module.builder()
                .spec("module:spec")
                .version("1-SNAPSHOT")
                .build();
        TinkerPopDependencyGraph graph = new TinkerPopDependencyGraph();

        graph.produces(repository, module);

        assertThat(graph.repositories(), arrayContainingInAnyOrder(repository));
        assertThat(graph.modules(), arrayContainingInAnyOrder(module));
        assertThat(graph.produced(repository), arrayContainingInAnyOrder(module));
    }

    @Test
    public void createDependsOn() throws Exception {
        Repository repository = Repository.builder()
                .id("repo")
                .name("repo")
                .checkoutSpec("checkout/spec/0")
                .build();
        Module module = Module.builder()
                .spec("module:spec")
                .version("1-SNAPSHOT")
                .build();
        TinkerPopDependencyGraph graph = new TinkerPopDependencyGraph();

        graph.dependsOn(repository, module);

        assertThat(graph.repositories(), arrayContainingInAnyOrder(repository));
        assertThat(graph.modules(), arrayContainingInAnyOrder(module));
        assertThat(graph.depending(module), arrayContainingInAnyOrder(repository));
        assertThat(graph.dependencies(repository), arrayContainingInAnyOrder(module));
    }

    @Test
    public void givenRepositoryHasDependencies__whenRepositoryDependencyAreRestted__thenRepositoryDependenciesAreEmpty() throws Exception {
        Repository repository = Repository.builder()
                .id("repo")
                .name("repo")
                .checkoutSpec("checkout/spec/0")
                .build();
        Module module = Module.builder()
                .spec("module:spec")
                .version("1-SNAPSHOT")
                .build();
        TinkerPopDependencyGraph graph = new TinkerPopDependencyGraph()
                .dependsOn(repository, module);


        graph.resetDependencies(repository);

        assertThat(graph.depending(module), is(emptyArray()));
        assertThat(graph.dependencies(repository), is(emptyArray()));
        assertThat(graph.repositoryById(repository.id()).isPresent(), is(true));
    }

    @Test
    public void givenRepositoryHasProduction__whenRepositoryProductionAreResetted__thenRepositoryProductionsAreEmpty() throws Exception {
        Repository repository = Repository.builder()
                .id("repo")
                .name("repo")
                .checkoutSpec("checkout/spec/0")
                .build();
        Module module = Module.builder()
                .spec("module:spec")
                .version("1-SNAPSHOT")
                .build();
        TinkerPopDependencyGraph graph = new TinkerPopDependencyGraph()
                .produces(repository, module);


        graph.resetProduced(repository);

        assertThat(graph.produced(repository), is(emptyArray()));
        assertThat(graph.repositoryById(repository.id()).isPresent(), is(true));
    }

    @Test
    public void downstream() throws Exception {
        Repository repo1 = Repository.builder()
                .id("repo1")
                .name("repo1")
                .checkoutSpec("checkout/spec/0")
                .build();
        Repository repo2 = Repository.builder()
                .id("repo2")
                .name("repo2")
                .checkoutSpec("checkout/spec/0")
                .build();
        Module module = Module.builder()
                .spec("module:spec")
                .version("1-SNAPSHOT")
                .build();
        DependencyGraph graph = new TinkerPopDependencyGraph()
                .produces(repo1, module)
                .dependsOn(repo2, module);

        Repository[] downstream = graph.downstream(repo1);
        for (Repository repository : downstream) {
            System.out.println(repository);
        }

        assertThat(downstream, is(arrayContainingInAnyOrder(repo2)));
    }

    @Test
    public void givenGraphExists__whenUpdatingGraph__thenGraphWithGivenIdIsUpdated() throws Exception {
        Repository repo = Repository.builder()
                .id("repo")
                .name("repo")
                .checkoutSpec("checkout/spec")
                .build();

        TinkerPopDependencyGraph graph = new TinkerPopDependencyGraph();
        graph.add(repo);

        Repository updated = graph.update(repo.withName("updated-name").withCheckoutSpec("updated/checkout/spec"));

        assertThat(updated, is(repo.withName("updated-name").withCheckoutSpec("updated/checkout/spec")));
        assertThat(graph.repositoryById(repo.id()).get(), is(repo.withName("updated-name").withCheckoutSpec("updated/checkout/spec")));
    }

    @Test
    public void givenGraphDoesntExist__whenUpdatingGraph__thenGraphIsCreated() throws Exception {
        TinkerPopDependencyGraph graph = new TinkerPopDependencyGraph();

        Repository repo = Repository.builder()
                .id("repo")
                .name("repo")
                .checkoutSpec("checkout/spec")
                .build();
        Repository updated = graph.update(repo);

        assertThat(updated, is(repo));
        assertThat(graph.repositoryById(repo.id()).get(), is(repo));
    }

    @Test
    public void produces() throws Exception {
        TinkerPopDependencyGraph graph = new TinkerPopDependencyGraph();

        Repository repo = Repository.builder()
                .id("repo")
                .name("repo")
                .checkoutSpec("checkout/spec")
                .build();


        graph.add(repo);

        graph.produces(repo,
                Module.builder().spec("a").version("1").build(),
                Module.builder().spec("b").version("1").build(),
                Module.builder().spec("c").version("1").build(),
                Module.builder().spec("d").version("1").build()
        );

        for (Module module : graph.produced(repo)) {
            System.out.println(module);
        }


        assertThat(graph.produced(repo), is(arrayContainingInAnyOrder(
                Module.builder().spec("a").version("1").build(),
                Module.builder().spec("b").version("1").build(),
                Module.builder().spec("c").version("1").build(),
                Module.builder().spec("d").version("1").build()
        )));
    }

    @Test
    public void dependsOn() throws Exception {
        TinkerPopDependencyGraph graph = new TinkerPopDependencyGraph();

        Repository repo = Repository.builder()
                .id("repo")
                .name("repo")
                .checkoutSpec("checkout/spec")
                .build();


        graph.add(repo);

        graph.dependsOn(repo,
                Module.builder().spec("a").version("1").build(),
                Module.builder().spec("b").version("1").build(),
                Module.builder().spec("c").version("1").build(),
                Module.builder().spec("d").version("1").build()
        );

        for (Module module : graph.dependencies(repo)) {
            System.out.println(module);
        }


        assertThat(graph.dependencies(repo), is(arrayContainingInAnyOrder(
                Module.builder().spec("a").version("1").build(),
                Module.builder().spec("b").version("1").build(),
                Module.builder().spec("c").version("1").build(),
                Module.builder().spec("d").version("1").build()
        )));
    }


    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    @Test
    public void producesAndDependsOn() throws Exception {
        File backupFile = this.dir.newFile();
        TinkerPopDependencyGraph graph = new TinkerPopDependencyGraph(backupFile);

        Repository repo = Repository.builder()
                .id("repo")
                .name("repo")
                .checkoutSpec("checkout/spec")
                .build();


        graph.add(repo);

        graph.produces(repo,
                Module.builder().spec("a").version("1").build(),
                Module.builder().spec("b").version("1").build()
        );
        graph.dependsOn(repo,
                Module.builder().spec("c").version("1").build(),
                Module.builder().spec("d").version("1").build()
        );

        for (Module module : graph.produced(repo)) {
            System.out.println(module);
        }


        assertThat(graph.produced(repo), is(arrayContainingInAnyOrder(
                Module.builder().spec("a").version("1").build(),
                Module.builder().spec("b").version("1").build()
        )));
        assertThat(graph.dependencies(repo), is(arrayContainingInAnyOrder(
                Module.builder().spec("c").version("1").build(),
                Module.builder().spec("d").version("1").build()
        )));
    }
}