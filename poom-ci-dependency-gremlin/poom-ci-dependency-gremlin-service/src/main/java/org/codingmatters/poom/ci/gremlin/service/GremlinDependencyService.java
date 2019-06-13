package org.codingmatters.poom.ci.gremlin.service;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Undertow;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.driver.ser.GryoMessageSerializerV3d0;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoMapper;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;
import org.janusgraph.graphdb.tinkerpop.JanusGraphIoRegistry;

import java.util.function.Supplier;

public class GremlinDependencyService {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(GremlinDependencyService.class);

    public static void main(String[] args) {
        GremlinDependencyService service = new GremlinDependencyService(
                new GremlinDependencyApi(new JsonFactory(), createConnectionSupplier()),
                Env.mandatory(Env.SERVICE_PORT).asInteger(),
                Env.mandatory(Env.SERVICE_HOST).asString()
        );
        service.start();

        log.info("poom-ci dependency api service [gremlin impl] running");
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        log.info("poom-ci dependency api service [gremlin impl] stopping...");
        service.stop();
        log.info("poom-ci dependency api service [gremlin impl] stopped.");
    }

    static public Supplier<RemoteConnection> createConnectionSupplier() {
        Cluster cluster = Cluster.build()
                .addContactPoint(Env.mandatory("GREMLIN_HOST").asString())
                .port(Env.optional("GREMLIN_PORT").orElse(new Env.Var("8182")).asInteger())
                .serializer(new GryoMessageSerializerV3d0(GryoMapper.build().addRegistry(JanusGraphIoRegistry.getInstance())))
                .create();
        return () -> DriverRemoteConnection.using(
                cluster,
                Env.optional("GREMLIN_GRAPH").orElse(new Env.Var("g")).asString()
        );
    }

    private final GremlinDependencyApi api;

    private Undertow server;
    private final int port;
    private final String host;

    public GremlinDependencyService(GremlinDependencyApi api, int port, String host) {
        this.port = port;
        this.host = host;
        this.api = api;
    }

    public void start() {
        this.server = Undertow.builder()
                .addHttpListener(this.port, this.host)
                .setHandler(new CdmHttpUndertowHandler(this.api.processor()))
                .build();
        this.server.start();
    }

    public void stop() {
        this.server.stop();
    }
}
