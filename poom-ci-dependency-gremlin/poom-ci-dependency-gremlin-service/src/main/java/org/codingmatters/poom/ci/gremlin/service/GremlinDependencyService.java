package org.codingmatters.poom.ci.gremlin.service;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Undertow;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

public class GremlinDependencyService {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(GremlinDependencyService.class);

    public static void main(String[] args) {
        DriverRemoteConnection connection = DriverRemoteConnection.using(
                Env.mandatory("GREMLIN_HOST").asString(),
                Env.optional("GREMLIN_PORT").orElse(new Env.Var("8182")).asInteger(),
                Env.optional("GREMLIN_GRAPH").orElse(new Env.Var("g")).asString()
        );
        GremlinDependencyService service = new GremlinDependencyService(
                new GremlinDependencyApi(new JsonFactory(), connection),
                Env.mandatory(Env.SERVICE_PORT).asInteger(),
                Env.mandatory(Env.SERVICE_HOST).asString()
        );
        service.start();

        log.info("poom-ci pipeline api service running");
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        log.info("poom-ci pipeline api service stopping...");
        service.stop();
        log.info("poom-ci pipeline api service stopped.");
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
