package org.codingmatters.poom.ci.runners;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
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
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.Executors;

public class PoomCIRunner {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PoomCIRunner.class);


    public static void main(String[] args) {
        KeyStore keystore = null;
        try {
            keystore = KeyStore.getInstance("pkcs12");
            keystore.load(new FileInputStream(Env.mandatory("KS").asString()), Env.mandatory("KS_PASS").asString().toCharArray());
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            log.error("error reading keystore poom-ci-runner", e);
            System.exit(1);
        }

        RunnerConfiguration configuration = new PoomCIRunner(
                Env.mandatory("JOB_REGISTRY_URL").asString(),
                Env.mandatory("RUNNER_REGISTRY_URL").asString(),
                Env.mandatory("PIPELINE_URL").asString(),
                Env.mandatory(Env.SERVICE_HOST).asString(),
                Env.mandatory(Env.SERVICE_PORT).asInteger(),
                2,
                keystore, Env.mandatory("KS_KEY_PASS").asString().toCharArray()
        ).buildConfiguration();

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
    private final KeyStore keystore;
    private final char[] keypass;

    public PoomCIRunner(String jobRegistryUrl, String runnerRegistryUrl, String pipelineUrl, String host, Integer port, int workerPoolSize, KeyStore keystore, char[] keypass) {
        this.jobRegistryUrl = jobRegistryUrl;
        this.runnerRegistryUrl = runnerRegistryUrl;
        this.pipelineUrl = pipelineUrl;
        this.host = host;
        this.port = port;
        this.workerPoolSize = workerPoolSize;
        this.keystore = keystore;
        this.keypass = keypass;
    }

    private RunnerConfiguration buildConfiguration() {
        JsonFactory jsonFactory = new JsonFactory();
        YAMLFactory yamlFactory = new YAMLFactory();

        RequesterFactory requesterFactory = new OkHttpRequesterFactory(OkHttpClientWrapper.build());

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

                .processorFactory(new PoomCIJobProcessorFactory(
                        pipelineAPIClient,
                        yamlFactory,
                        this.keystore,
                        this.keypass,
                        jsonFactory))

                .jobCategory("poom-ci")
                .jobName(
                        PoomCIJobProcessorFactory.triggerJobName(PipelineTrigger.Type.GITHUB_PUSH),
                        PoomCIJobProcessorFactory.triggerJobName(PipelineTrigger.Type.UPSTREAM_BUILD)
                )

                .jobRegistryAPIClient(jobRegistryAPIClient)
                .runnerRegistryAPIClient(runnerRegistryApi)

                .jobWorker(Executors.newFixedThreadPool(this.workerPoolSize))

                .build();
    }
}
