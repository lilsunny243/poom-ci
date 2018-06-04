package org.codingmatters.poom.ci.service.bundle;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Undertow;
import okhttp3.OkHttpClient;
import org.codingmatters.poom.ci.dependency.api.service.DependencyApi;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;
import org.codingmatters.poom.ci.pipeline.api.service.PoomCIApi;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.client.PoomjobsJobRegistryAPIRequesterClient;
import org.codingmatters.poom.client.PoomjobsRunnerRegistryAPIHandlersClient;
import org.codingmatters.poom.poomjobs.domain.jobs.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.runners.repositories.RunnerRepository;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.runner.manager.RunnerInvokerListener;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.poomjobs.service.PoomjobsJobRegistryAPI;
import org.codingmatters.poomjobs.service.PoomjobsRunnerRegistryAPI;
import org.codingmatters.poomjobs.service.api.PoomjobsJobRegistryAPIProcessor;
import org.codingmatters.poomjobs.service.api.PoomjobsRunnerRegistryAPIProcessor;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.codingmatters.rest.api.processors.MatchingPathProcessor;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class PoomCIApisService {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PoomCIApisService.class);

    public static void main(String[] args) {
        String host = Env.mandatory(Env.SERVICE_HOST).asString();
        int port = Env.mandatory(Env.SERVICE_PORT).asInteger();
        int clientPoolSize = Env.optional("CLIENT_POOL_SIZE").orElse(new Env.Var("5")).asInteger();

        JsonFactory jsonFactory = new JsonFactory();

        AtomicInteger threadIndex = new AtomicInteger(1);
        ExecutorService clientPool = Executors.newFixedThreadPool(clientPoolSize,
                runnable -> new Thread(runnable, "client-pool-thread-" + threadIndex.getAndIncrement())
        );

        PoomjobsRunnerRegistryAPI runnerRegistryAPI = runnerRegistryAPI();
        PoomCIApisService service = new PoomCIApisService(host, port, jsonFactory,
                poomCIApi(jsonFactory),
                dependencyApi(jsonFactory),
                runnerRegistryAPI,
                jobRegistryAPI(runnerRegistryAPI, clientPool)
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

    static public PoomjobsRunnerRegistryAPI runnerRegistryAPI() {
        Repository<RunnerValue, RunnerQuery> runnerRepository = RunnerRepository.createInMemory();
        return new PoomjobsRunnerRegistryAPI(runnerRepository);
    }

    static public PoomjobsJobRegistryAPI jobRegistryAPI(PoomjobsRunnerRegistryAPI runnerRegistryApi, ExecutorService clientPool) {
        Repository<JobValue, JobQuery> jobRepository = JobRepository.createInMemory();
        PoomjobsRunnerRegistryAPIHandlersClient runnerRegistryClient = new PoomjobsRunnerRegistryAPIHandlersClient(
                runnerRegistryApi.handlers(),
                clientPool
        );
        return new PoomjobsJobRegistryAPI(
                jobRepository,
                new RunnerInvokerListener(runnerRegistryClient)
        );
    }


    private Undertow server;
    private final int port;
    private final String host;
    private final JsonFactory jsonFactory;

    private final PoomCIApi poomCIApi;
    private final DependencyApi dependencyApi;
    private final PoomjobsRunnerRegistryAPI runnerRegistryAPI;
    private final PoomjobsJobRegistryAPI jobRegistryAPI;

    public PoomCIApisService(String host, int port, JsonFactory jsonFactory, PoomCIApi poomCIApi, DependencyApi dependencyApi, PoomjobsRunnerRegistryAPI runnerRegistryAPI, PoomjobsJobRegistryAPI jobRegistryAPI) {
        this.port = port;
        this.host = host;
        this.jsonFactory = jsonFactory;
        this.poomCIApi = poomCIApi;
        this.dependencyApi = dependencyApi;
        this.runnerRegistryAPI = runnerRegistryAPI;
        this.jobRegistryAPI = jobRegistryAPI;
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
                .whenMatching("/poomjobs-jobs/v1/.*", new PoomjobsJobRegistryAPIProcessor(
                        "/poomjobs-jobs/v1",
                        this.jsonFactory,
                        this.jobRegistryAPI.handlers()
                ))
                .whenMatching("/poomjobs-runners/v1/.*", new PoomjobsRunnerRegistryAPIProcessor(
                        "/poomjobs-runners/v1",
                        this.jsonFactory,
                        this.runnerRegistryAPI.handlers()
                ))
                .whenNoMatch((request, response) ->
                        response.status(404).payload("{\"code\":\"NOT_FOUND\"}", "UTF-8")
                );
    }

    public void stop() {
        this.server.stop();
    }
}
