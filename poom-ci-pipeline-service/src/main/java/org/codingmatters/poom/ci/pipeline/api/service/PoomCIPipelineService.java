package org.codingmatters.poom.ci.pipeline.api.service;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Undertow;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PeriodicalOperator;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.repository.StageLogSegmentedRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLogQuery;
import org.codingmatters.poom.client.PoomjobsJobRegistryAPIRequesterClient;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        PeriodicalOperator<PoomCIRepository.StageLogKey, StageLog, StageLogQuery> logStorer;
        PeriodicalOperator<PoomCIRepository.StageLogKey, StageLog, StageLogQuery> logPurger;

        PoomCIRepository repository = null;
        try {
            StageLogSegmentedRepository logRepository = new StageLogSegmentedRepository(logStorage, jsonFactory);

            logStorer = new PeriodicalOperator<>(
                    logRepository,
                    PeriodicalOperator.PeriodicalOperations.storer(),
                    scheduler,
                    Env.optional("STORE_TICK").orElse(new Env.Var("30")).asLong(),
                    TimeUnit.SECONDS
            );
            logStorer.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    logStorer.stop();
                } catch (Exception e) {
                    throw new RuntimeException("failed stopping log storer", e);
                }
            }));

            logPurger = new PeriodicalOperator<>(
                    logRepository,
                    PeriodicalOperator.PeriodicalOperations.purger(Env.optional("PURGE_TTL").orElse(new Env.Var("120")).asLong()),
                    scheduler,
                    Env.optional("PURGE_TICK").orElse(new Env.Var("240")).asLong(),
                    TimeUnit.SECONDS
            );
            logPurger.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    logPurger.stop();
                } catch (Exception e) {
                    throw new RuntimeException("failed stopping log purger", e);
                }
            }));

            repository = PoomCIRepository.inMemory(logRepository);
        } catch (IOException e) {
            throw new RuntimeException("error creating log storage", e);
        }

        String jobRegistryUrl = Env.mandatory("JOB_REGISTRY_URL").asString();
        return new PoomCIApi(repository, "/pipelines", jsonFactory, new PoomjobsJobRegistryAPIRequesterClient(
                new OkHttpRequesterFactory(OkHttpClientWrapper.build()), jsonFactory, jobRegistryUrl
        ));
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
