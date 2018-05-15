package org.codingmatters.poom.ci.pipeline.api.service;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Undertow;
import okhttp3.OkHttpClient;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.client.PoomjobsJobRegistryAPIRequesterClient;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

public class PoomCIPipelineService {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PoomCIPipelineService.class);

    public static void main(String[] args) {
        String host = Env.mandatory(Env.SERVICE_HOST).asString();
        int port = Env.mandatory(Env.SERVICE_PORT).asInteger();
        String jobRegistryUrl = Env.mandatory("JOB_REGISTRY_URL").asString();

        JsonFactory jsonFactory = new JsonFactory();
        PoomCIRepository repository = PoomCIRepository.inMemory();

        PoomCIPipelineService service = new PoomCIPipelineService(jsonFactory, repository, port, host, jobRegistryUrl);
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
    private final PoomCIRepository repository;
    private final PoomCIApi api;

    private Undertow server;
    private final int port;
    private final String host;

    public PoomCIPipelineService(JsonFactory jsonFactory, PoomCIRepository repository, int port, String host, String jobRegistryUrl) {
        this.jsonFactory = jsonFactory;
        this.repository = repository;
        this.port = port;
        this.host = host;
        this.api = new PoomCIApi(repository, "/pipelines/v1", jsonFactory, new PoomjobsJobRegistryAPIRequesterClient(
                new OkHttpRequesterFactory(new OkHttpClient()), this.jsonFactory, jobRegistryUrl
        ));
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
