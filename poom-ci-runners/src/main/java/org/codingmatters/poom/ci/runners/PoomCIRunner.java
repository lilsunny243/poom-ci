package org.codingmatters.poom.ci.runners;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import okhttp3.OkHttpClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIRequesterClient;
import org.codingmatters.poom.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poom.client.PoomjobsJobRegistryAPIRequesterClient;
import org.codingmatters.poom.client.PoomjobsRunnerRegistryAPIClient;
import org.codingmatters.poom.client.PoomjobsRunnerRegistryAPIRequesterClient;
import org.codingmatters.poom.runner.GenericRunner;
import org.codingmatters.poom.runner.configuration.RunnerConfiguration;
import org.codingmatters.poom.runner.exception.RunnerInitializationException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.client.RequesterFactory;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

import java.util.concurrent.Executors;

public class PoomCIRunner {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PoomCIRunner.class);


    public static void main(String[] args) {
        RunnerConfiguration configuration = new PoomCIRunner(
                Env.mandatory("JOB_REGISTRY_URL"),
                Env.mandatory("RUNNER_REGISTRY_URL"),
                Env.mandatory("PIPELINE_URL"),
                Env.mandatory(Env.SERVICE_HOST),
                Integer.parseInt(Env.mandatory(Env.SERVICE_PORT)),
                2).buildConfiguration();

        GenericRunner runner = new GenericRunner(configuration);
        try {
            runner.start();
        } catch (RunnerInitializationException e) {
            log.error("error starting poom-ci-runner", e);
            System.exit(1);
        }

        log.info("poom-ci runner started");
        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        log.info("poom-ci runner stopped");
    }

    private final String jobRegistryUrl;
    private final String runnerRegistryUrl;
    private final String pipelineUrl;
    private final String host;
    private final Integer port;
    private final int workerPoolSize;

    public PoomCIRunner(String jobRegistryUrl, String runnerRegistryUrl, String pipelineUrl, String host, Integer port, int workerPoolSize) {
        this.jobRegistryUrl = jobRegistryUrl;
        this.runnerRegistryUrl = runnerRegistryUrl;
        this.pipelineUrl = pipelineUrl;
        this.host = host;
        this.port = port;
        this.workerPoolSize = workerPoolSize;
    }

    private RunnerConfiguration buildConfiguration() {
        JsonFactory jsonFactory = new JsonFactory();
        YAMLFactory yamlFactory = new YAMLFactory();

        RequesterFactory requesterFactory = new OkHttpRequesterFactory(new OkHttpClient());

        PoomjobsJobRegistryAPIClient jobRegistryAPIClient = new PoomjobsJobRegistryAPIRequesterClient(
                requesterFactory,
                jsonFactory,
                this.jobRegistryUrl
        );

        PoomjobsRunnerRegistryAPIClient runnerRegistryApi = new PoomjobsRunnerRegistryAPIRequesterClient(
                requesterFactory,
                jsonFactory,
                this.runnerRegistryUrl
        );

        PoomCIPipelineAPIClient pipelineAPIClient = new PoomCIPipelineAPIRequesterClient(
                requesterFactory,
                jsonFactory,
                this.pipelineUrl
        );
        return RunnerConfiguration.builder()
                .jobRegistryUrl(this.jobRegistryUrl)
                .endpointHost(this.host)
                .endpointPort(this.port)

                .callbackBaseUrl(String.format("http://%s:%s", this.host, this.port))
                .ttl(2 * 60 * 1000L)

                .processorFactory(new PoomCIJobProcessorFactory(pipelineAPIClient, yamlFactory))

                .jobCategory("poom-ci")
                .jobName("github-pipeline")

                .jobRegistryAPIClient(jobRegistryAPIClient)
                .runnerRegistryAPIClient(runnerRegistryApi)

                .jobWorker(Executors.newFixedThreadPool(this.workerPoolSize))

                .build();
    }
}
