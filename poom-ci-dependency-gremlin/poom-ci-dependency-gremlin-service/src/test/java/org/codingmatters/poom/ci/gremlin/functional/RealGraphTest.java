package org.codingmatters.poom.ci.gremlin.functional;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import io.flexio.docker.DockerResource;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.codingmatters.poom.ci.dependency.api.RepositoryPutRequest;
import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.api.types.ValueList;
import org.codingmatters.poom.ci.dependency.api.types.json.FullRepositoryReader;
import org.codingmatters.poom.ci.dependency.api.types.json.ModuleReader;
import org.codingmatters.poom.ci.gremlin.GremlinResource;
import org.codingmatters.poom.ci.gremlin.RealGraphLoader;
import org.codingmatters.poom.ci.gremlin.queries.DownstreamQuery;
import org.codingmatters.poom.ci.gremlin.queries.NextDownstreamsQuery;
import org.codingmatters.poom.ci.gremlin.queries.RepositoryQuery;
import org.codingmatters.poom.ci.gremlin.service.Mappers;
import org.codingmatters.poom.ci.gremlin.service.handlers.CreateOrUpdateRepository;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class RealGraphTest {

    static public final DockerResource docker = GremlinResource.withGremlinContainer(DockerResource.client());
    static public final GremlinResource gremlin = new GremlinResource(docker);

    @ClassRule
    static public TestRule chain= RuleChain
            .outerRule(docker)
            .around(gremlin);

    @BeforeClass
    static public void loadGraph() throws Exception {
        RealGraphLoader.load("real-sample-2019-03-15", gremlin);
    }

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void givenGraphLoaded__whenListingRepositories__thenTheres9() throws Exception {
        List<Repository> repositories = new RepositoryQuery<Repository>(AnonymousTraversalSource.traversal().withRemote(gremlin.remoteConnection()), Mappers::repository).all();
        assertThat(repositories, hasSize(9));
    }

    @Test
    public void downstream() throws Exception {
        // flexiooss-codingmatters-rest-develop/downstream

        List<String> downstreams = new DownstreamQuery<>(
                AnonymousTraversalSource.traversal().withRemote(gremlin.remoteConnection()),
                Mappers::repository)
                .forRepository("flexiooss-codingmatters-rest-develop").stream().map(Repository::id).collect(Collectors.toList());

        System.out.println("DOWNSTREAMS :: " + downstreams);

        assertThat(downstreams, hasSize(4));
        assertThat(
                downstreams,
                containsInAnyOrder(
                        "flexiooss-poom-services-develop",
                        "flexiooss-poomjobs-develop",
                        "flexiooss-poom-ci-develop",
                        "flexiooss-flexio-commons-develop")
        );
    }

    @Test
    public void nextDownstream() throws Exception {
        List<String> downstreams = new NextDownstreamsQuery<>(AnonymousTraversalSource.traversal().withRemote(gremlin.remoteConnection()),
                Mappers::repository)
                .forRepository("flexiooss-codingmatters-rest-develop").stream().map(Repository::id).collect(Collectors.toList());

        System.out.println("DOWNSTREAMS :: " + downstreams);

        assertThat(downstreams, hasSize(1));
        assertThat(
                downstreams,
                containsInAnyOrder(
                        "flexiooss-poom-services-develop"
                )
        );
    }
}
