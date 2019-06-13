package org.codingmatters.poom.ci.gremlin.service.handlers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import io.flexio.docker.DockerResource;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.codingmatters.poom.ci.dependency.api.RepositoryPutRequest;
import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.api.types.json.FullRepositoryReader;
import org.codingmatters.poom.ci.gremlin.GremlinResource;
import org.codingmatters.poom.ci.gremlin.queries.DependenciesQuery;
import org.codingmatters.poom.ci.gremlin.queries.ProducedByQuery;
import org.codingmatters.poom.ci.gremlin.queries.RepositoryQuery;
import org.codingmatters.poom.ci.gremlin.service.Mappers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CreateOrUpdateRepositoryTest {

    @ClassRule
    static public final DockerResource docker = GremlinResource.withGremlinContainer(DockerResource.client());

    @Rule
    public GremlinResource gremlin = new GremlinResource(docker);

    private final JsonFactory jsonFactory = new JsonFactory();

    @Test
    public void givenDependencyGraphIsEmpty__whenPuttingFullRepository__thenGraphIsLoaded() throws Exception {
        new CreateOrUpdateRepository(this.gremlin.remoteConnectionSupplier()).apply(RepositoryPutRequest.builder()
                .repositoryId("REPOID")
                .payload(this.fromJson("full-repository.json"))
                .build());

        RepositoryQuery<Repository> repoQuery = new RepositoryQuery<>(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()), Mappers::repository);

        assertThat(repoQuery.all(), hasSize(1));
        assertThat(
                repoQuery.repository("REPOID").get(),
                is(Repository.builder()
                        .id("REPOID")
                        .name("REPONAME")
                        .checkoutSpec("REPOSPEC")
                        .build())
        );

        DependenciesQuery<Module> deps = new DependenciesQuery<>(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()), Mappers::module);
        assertThat(deps.forRepository("REPOID"), hasSize(120));

        ProducedByQuery<Module> produced = new ProducedByQuery<>(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()), Mappers::module);
        assertThat(produced.forRepository("REPOID"), hasSize(23));
    }

    @Test
    public void givenDependencyIsProduced__whenCreatingRepository__thenProducedDependencyIsNotStoredAsADependency() throws Exception {
        new CreateOrUpdateRepository(this.gremlin.remoteConnectionSupplier()).apply(RepositoryPutRequest.builder()
                .repositoryId("REPOID")
                .payload(repo -> repo
                        .id("REPOID")
                        .name("REPONAME")
                        .checkoutSpec("REPOSPEC")
                        .dependencies(module -> module.spec("group:module").version("1"))
                        .produces(module -> module.spec("group:module").version("1"))
                )
                .build());

        DependenciesQuery<Module> deps = new DependenciesQuery<>(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()), Mappers::module);
        assertThat(deps.forRepository("REPOID"), hasSize(0));

        ProducedByQuery<Module> produced = new ProducedByQuery<>(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()), Mappers::module);
        assertThat(produced.forRepository("REPOID"), hasSize(1));
    }

    @Test
    public void givenDependencyIsDuplicated__whenCreatingRepository__thenDependencyIsStoredOnce() throws Exception {
        new CreateOrUpdateRepository(this.gremlin.remoteConnectionSupplier()).apply(RepositoryPutRequest.builder()
                .repositoryId("REPOID")
                .payload(repo -> repo
                        .id("REPOID")
                        .name("REPONAME")
                        .checkoutSpec("REPOSPEC")
                        .dependencies(
                                module -> module.spec("group:module").version("1"),
                                module -> module.spec("group:module").version("1")
                        )
                )
                .build());

        DependenciesQuery<Module> deps = new DependenciesQuery<>(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()), Mappers::module);
        assertThat(deps.forRepository("REPOID"), hasSize(1));
    }

    @Test
    public void givenProducedIsDuplicated__whenCreatingRepository__thenProducedIsStoredOnce() throws Exception {
        new CreateOrUpdateRepository(this.gremlin.remoteConnectionSupplier()).apply(RepositoryPutRequest.builder()
                .repositoryId("REPOID")
                .payload(repo -> repo
                        .id("REPOID")
                        .name("REPONAME")
                        .checkoutSpec("REPOSPEC")
                        .produces(
                                module -> module.spec("group:module").version("1"),
                                module -> module.spec("group:module").version("1")
                        )
                )
                .build());

        ProducedByQuery<Module> produced = new ProducedByQuery<>(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()), Mappers::module);
        assertThat(produced.forRepository("REPOID"), hasSize(1));
    }

    private FullRepository fromJson(String resource) throws IOException {
        try(JsonParser parser = this.jsonFactory.createParser(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))) {
            return new FullRepositoryReader().read(parser);
        }
    }
}