package org.codingmatters.poom.ci.dependency.api.service;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Undertow;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

import java.io.File;
import java.io.IOException;

public class PoomCIDependencyService {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PoomCIDependencyService.class);

    public static void main(String[] args) {
        String host = Env.mandatory(Env.SERVICE_HOST).asString();
        int port = Env.mandatory(Env.SERVICE_PORT).asInteger();
        File graphFile = new File(Env.mandatory("GRAPH_STORAGE_FILE").asString());

        JsonFactory jsonFactory = new JsonFactory();
        PoomCIDependencyService service = null;
        try {
            service = new PoomCIDependencyService(jsonFactory, port, host,
                    new DependencyApi(jsonFactory, "/dependencies", new DependencyGraph(graphFile))
            );
        } catch (IOException e) {
            log.error("error creating dependency graph", e);
        }
        service.start();

        log.info("poom-ci pipeline api service running");
        while(true) {
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

    private final JsonFactory jsonFactory;

    private Undertow server;
    private final int port;
    private final String host;
    private final DependencyApi api;

    public PoomCIDependencyService(JsonFactory jsonFactory, int port, String host, DependencyApi api) {
        this.jsonFactory = jsonFactory;
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
