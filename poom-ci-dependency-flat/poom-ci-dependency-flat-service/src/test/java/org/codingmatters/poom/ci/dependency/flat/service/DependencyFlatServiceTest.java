package org.codingmatters.poom.ci.dependency.flat.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.mongodb.MongoClient;
import io.flexio.docker.DockerResource;
import io.flexio.services.tests.mongo.MongoResource;
import io.flexio.services.tests.mongo.MongoTest;
import org.codingmatters.poom.ci.dependency.api.PoomCIDependencyAPIDescriptor;
import org.codingmatters.poom.ci.dependency.api.RepositoriesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoriesGetResponse;
import org.codingmatters.poom.ci.dependency.api.RepositoryGetRequest;
import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.client.PoomCIDependencyAPIClient;
import org.codingmatters.poom.ci.dependency.client.PoomCIDependencyAPIRequesterClient;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DependencyFlatServiceTest {
    public static final String DB = "test-db";

    @ClassRule
    static public DockerResource docker = MongoTest.docker()
            .started().finallyStarted();

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
        this.service = new DependencyFlatService(graphManager, "localhost", port, new JsonFactory());
        this.service.start();

        String url = "http://localhost:" + port + "/" + PoomCIDependencyAPIDescriptor.NAME;
        this.dependencies = new PoomCIDependencyAPIRequesterClient(new OkHttpRequesterFactory(OkHttpClientWrapper.build(), () -> url),
                new JsonFactory(),
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
}