package org.codingmatters.poom.ci.pipeline.api.service;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Undertow;
import org.codingmatters.poom.ci.pipeline.api.service.repository.LogFileStore;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.client.PoomjobsJobRegistryAPIRequesterClient;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

import java.io.File;

public class PoomCIPipelineService {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PoomCIPipelineService.class);

    public static void main(String[] args) {
        String host = Env.mandatory(Env.SERVICE_HOST).asString();
        int port = Env.mandatory(Env.SERVICE_PORT).asInteger();

        PoomCIPipelineService service = new PoomCIPipelineService(api(), port, host);
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

    static public PoomCIApi api() {
        JsonFactory jsonFactory = new JsonFactory();

        File logStorage = new File(Env.mandatory("LOG_STORAGE").asString());

        PoomCIRepository repository = PoomCIRepository.inMemory(new LogFileStore(logStorage));
        String jobRegistryUrl = Env.mandatory("JOB_REGISTRY_URL").asString();
        return new PoomCIApi(repository, "/pipelines", jsonFactory, new PoomjobsJobRegistryAPIRequesterClient(
                new OkHttpRequesterFactory(OkHttpClientWrapper.build(), () -> jobRegistryUrl), jsonFactory, jobRegistryUrl)
        );
    }

    private final PoomCIApi api;

    private Undertow server;
    private final int port;
    private final String host;

    public PoomCIPipelineService(PoomCIApi api, int port, String host) {
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
