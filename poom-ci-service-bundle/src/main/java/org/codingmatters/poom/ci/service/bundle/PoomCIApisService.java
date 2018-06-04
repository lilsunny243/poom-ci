package org.codingmatters.poom.ci.service.bundle;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Undertow;
import okhttp3.OkHttpClient;
import org.codingmatters.poom.ci.dependency.api.service.DependencyApi;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;
import org.codingmatters.poom.ci.pipeline.api.service.PoomCIApi;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.client.PoomjobsJobRegistryAPIRequesterClient;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.codingmatters.rest.api.processors.MatchingPathProcessor;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

import java.io.File;
import java.io.IOException;

public class PoomCIApisService {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PoomCIApisService.class);

    public static void main(String[] args) {
        String host = Env.mandatory(Env.SERVICE_HOST).asString();
        int port = Env.mandatory(Env.SERVICE_PORT).asInteger();

        JsonFactory jsonFactory = new JsonFactory();

        PoomCIApisService service = new PoomCIApisService(host, port,
                poomCIApi(jsonFactory),
                dependencyApi(jsonFactory)
        );
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

    private static PoomCIApi poomCIApi(JsonFactory jsonFactory) {
        String jobRegistryUrl = Env.mandatory("JOB_REGISTRY_URL").asString();
        PoomCIRepository repository = PoomCIRepository.inMemory();
        return new PoomCIApi(repository, "/pipelines", jsonFactory, new PoomjobsJobRegistryAPIRequesterClient(
                new OkHttpRequesterFactory(new OkHttpClient()), jsonFactory, jobRegistryUrl)
        );
    }

    private static DependencyApi dependencyApi(JsonFactory jsonFactory) {
        File graphFile = new File(Env.mandatory("GRAPH_STORAGE_FILE").asString());
        DependencyGraph dependencyGraph = null;
        try {
            dependencyGraph = new DependencyGraph(graphFile);
        } catch (IOException e) {
            throw new RuntimeException("error creating dependency service graph storage", e);
        }
        return new DependencyApi(jsonFactory, "/dependencies", dependencyGraph);
    }


    private Undertow server;
    private final int port;
    private final String host;

    private final PoomCIApi poomCIApi;
    private final DependencyApi dependencyApi;

    public PoomCIApisService(String host, int port, PoomCIApi poomCIApi, DependencyApi dependencyApi) {
        this.port = port;
        this.host = host;
        this.poomCIApi = poomCIApi;
        this.dependencyApi = dependencyApi;
    }

    public void start() {
        this.server = Undertow.builder()
                .addHttpListener(this.port, this.host)
                .setHandler(new CdmHttpUndertowHandler(this.processor()))
                .build();
        this.server.start();
    }

    private Processor processor() {
        return MatchingPathProcessor
                .whenMatching(this.poomCIApi.path() + "/.*", this.poomCIApi.processor())
                .whenMatching(this.dependencyApi.path() + "/.*", this.dependencyApi.processor())
                .whenNoMatch((request, response) ->
                        response.status(404).payload("{\"code\":\"NOT_FOUND\"}", "UTF-8")
                );
    }

    public void stop() {
        this.server.stop();
    }
}
