package org.codingmatters.poom.ci.dependency.flat.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.mongodb.MongoClient;
import io.flexio.docker.DockerResource;
import io.flexio.services.tests.mongo.MongoResource;
import io.flexio.services.tests.mongo.MongoTest;
import org.codingmatters.poom.ci.dependency.api.*;
import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.api.types.json.ModuleReader;
import org.codingmatters.poom.ci.dependency.api.types.json.RepositoryReader;
import org.codingmatters.poom.ci.dependency.client.PoomCIDependencyAPIClient;
import org.codingmatters.poom.ci.dependency.client.PoomCIDependencyAPIRequesterClient;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;
import org.codingmatters.poom.ci.dependency.flat.GraphManagerException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Ignore
public class DependencyFlatServiceTest {
    public static final String DB = "test-db";

    @ClassRule
    static public DockerResource docker = MongoTest.docker()
            .started().finallyStarted();
    private final JsonFactory jsonFactory = new JsonFactory();

    @Rule
    public MongoResource mongo = MongoTest.mongo(() -> docker)
            .testDB(DB);
    private MongoClient serviceClient;
    private DependencyFlatService service;

    private PoomCIDependencyAPIClient dependencies;
    private GraphManager graphManager;

    @Before
    public void setUp() throws Exception {
        this.serviceClient = this.mongo.newClient();
        this.graphManager = new GraphManager(
                DependencyFlatService.repositoriesMongoRepository(this.serviceClient, DB),
                DependencyFlatService.producesRelationRepository(this.serviceClient, DB),
                DependencyFlatService.dependsOnRelationRepository(this.serviceClient, DB)
        );
        int port = this.unusedPort();
        this.service = new DependencyFlatService(graphManager, "localhost", port, this.jsonFactory);
        this.service.start();

        String url = "http://localhost:" + port + "/" + PoomCIDependencyAPIDescriptor.NAME;
        this.dependencies = new PoomCIDependencyAPIRequesterClient(new OkHttpRequesterFactory(OkHttpClientWrapper.build(), () -> url),
                this.jsonFactory,
                () -> url);
    }

    private int unusedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @After
    public void tearDown() throws Exception {
        this.service.stop();
    }

    @Test
    public void givenRepositoryIsIndexed__whenGettingRepository__then200_andRepoReturned() throws Exception {
        this.graphManager.index(FullRepository.builder().id("a-repo").checkoutSpec("a/repo").name("ARepo").build());

        assertThat(
                this.dependencies.repositories().repository().get(RepositoryGetRequest.builder().repositoryId("a-repo").build()).status200().payload(),
                is(this.graphManager.repository("a-repo").get())
        );
    }

    @Test
    public void microBench() throws Exception {
        long indexingStart = System.currentTimeMillis();
        this.indexSample();
        long indexingEnd = System.currentTimeMillis();

        long listingStart = System.currentTimeMillis();
        this.dependencies.repositories().get(RepositoriesGetRequest.builder().build()).status200().payload();
        long listingEnd = System.currentTimeMillis();

        long graphStart = System.currentTimeMillis();
        System.out.println("GRAPH         :::" + this.dependencies.repositoryGraph().get(RepositoryGraphGetRequest.builder().root("flexiooss-codingmatters-reflect-unit-develop").build()).status200().payload());
        long graphEnd = System.currentTimeMillis();

        System.out.println("INDEXING TOOK ::: " + (indexingEnd - indexingStart) + "ms.");
        System.out.println("LISTING TOOK  ::: " + (listingEnd - listingStart) + "ms.");
        System.out.println("GRAPH TOOK    ::: " + (graphEnd - graphStart) + "ms.");
    }

    public void indexSample() throws IOException, GraphManagerException {
        for (Repository repo : this.readRepos()) {
            this.graphManager.index(FullRepository.builder()
                    .id(repo.id())
                    .checkoutSpec(repo.checkoutSpec())
                    .name(repo.name())
                    .produces(this.readModules(repo, "produces"))
                    .dependencies(this.readModules(repo, "depends-on"))
                    .build());
        }
    }

    private Repository[] readRepos() throws IOException {
        try(JsonParser parser = this.jsonFactory.createParser(Thread.currentThread().getContextClassLoader().getResourceAsStream("real-sample-2019-06-03/repositories.json"))) {
             return new RepositoryReader().readArray(parser);
        }
    }

    private Module[] readModules(Repository repo, String dir) throws IOException {
        try(JsonParser parser = this.jsonFactory.createParser(Thread.currentThread().getContextClassLoader().getResourceAsStream("real-sample-2019-06-03/" + dir + "/" + repo.id() + ".json"))) {
            return new ModuleReader().readArray(parser);
        }
    }
}