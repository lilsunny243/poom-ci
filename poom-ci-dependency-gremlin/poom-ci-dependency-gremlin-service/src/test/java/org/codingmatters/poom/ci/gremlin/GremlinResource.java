package org.codingmatters.poom.ci.gremlin;

import io.flexio.docker.DockerResource;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.driver.ser.GryoMessageSerializerV3d0;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnectionException;
import org.apache.tinkerpop.gremlin.process.remote.traversal.RemoteTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.Bytecode;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoMapper;
import org.janusgraph.graphdb.tinkerpop.JanusGraphIoRegistry;
import org.junit.rules.ExternalResource;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class GremlinResource extends ExternalResource {

    static public DockerResource withGremlinContainer(DockerResource docker) {
        return docker
                .with(
                        "gremlin",
                        container -> container
                                .image("codingmatters/gremlin-server-janus-berkeleyje:0.4.0")
                ).started()
                .finallyStarted();
//                .finallyStopped();
    }

    private final DockerResource docker;

    private DriverRemoteConnection remoteConnection;

    public GremlinResource(DockerResource docker) {
        this.docker = docker;
    }

    public DriverRemoteConnection remoteConnection() {
        return remoteConnection;
    }

    @Override
    protected void before() throws Throwable {
        super.before();

        Cluster.Builder builder = Cluster.build()
                .addContactPoint(docker.containerInfo("gremlin").get().networkSettings().iPAddress())
                .port(8182)
                .serializer(new GryoMessageSerializerV3d0(GryoMapper.build().addRegistry(JanusGraphIoRegistry.getInstance())));

        this.remoteConnection = DriverRemoteConnection.using(builder.create(), "g");

//        this.remoteConnection = DriverRemoteConnection.using(
//                docker.containerInfo("gremlin").get().networkSettings().iPAddress(),
//                8182,
//                "g");

        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(remoteConnection);
        boolean ready = false;
        int connectTried = 0;
        int connectMaxTry = 10;
        IllegalStateException lastException = null;
        while((! ready) && connectTried < connectMaxTry) {
            try {
                g.E().drop().iterate();
                g.V().drop().iterate();
                ready = true;
            } catch (IllegalStateException e) {
                lastException = e;
                connectTried++;
                Thread.sleep(1000);
            }
        }

        if(! ready) {
            throw new AssertionError("failed to initialize connection to gremlin" + this.remoteConnection, lastException);
        }
    }

    @Override
    protected void after() {
        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(remoteConnection);
        g.E().drop().iterate();
        g.V().drop().iterate();
        super.after();
    }

    public Supplier<RemoteConnection> remoteConnectionSupplier() {
        return () -> new RemoteConnection() {
            @Override
            public <E> CompletableFuture<RemoteTraversal<?, E>> submitAsync(Bytecode bytecode) throws RemoteConnectionException {
                return remoteConnection.submitAsync(bytecode);
            }

            @Override
            public void close() throws Exception {
            }
        };
    }
}
