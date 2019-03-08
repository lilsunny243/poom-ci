package org.codingmatters.poom.ci.gremlin;

import io.flexio.docker.DockerResource;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.rules.ExternalResource;

public class GremlinResource extends ExternalResource {

    static public DockerResource withGremlinContainer(DockerResource docker) {
        return docker
                .with(
                        "gremlin",
                        container -> container
                                .image("tinkerpop/gremlin-server:3.4.0")
                ).started().finallyStopped();
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

        this.remoteConnection = DriverRemoteConnection.using(
                docker.containerInfo("gremlin").get().networkSettings().iPAddress(),
                8182,
                "g");

        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(remoteConnection);
        boolean ready = false;
        int connectTried = 0;
        int connectMaxTry = 10;
        IllegalStateException lastException = null;
        while((! ready) && connectTried < connectMaxTry) {
            try {
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
        g.V().drop().iterate();
        super.after();
    }
}
