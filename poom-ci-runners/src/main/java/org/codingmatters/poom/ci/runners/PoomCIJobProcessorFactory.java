package org.codingmatters.poom.ci.runners;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.runners.pipeline.PipelineContext;
import org.codingmatters.poom.ci.runners.pipeline.PipelineExecutor;
import org.codingmatters.poom.ci.runners.pipeline.PipelineJobProcessor;
import org.codingmatters.poom.ci.runners.pipeline.executors.PipelineShellExecutor;
import org.codingmatters.poom.ci.runners.pipeline.providers.GithubPipelineContextProvider;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poomjobs.api.types.Job;

import java.security.KeyStore;

public class PoomCIJobProcessorFactory implements JobProcessor.Factory {

    private final PoomCIPipelineAPIClient pipelineClient;
    private final YAMLFactory yamlFactory;
    private final KeyStore keystore;
    private final char[] keypass;
    private final JsonFactory jsonFactory;

    public PoomCIJobProcessorFactory(PoomCIPipelineAPIClient pipelineClient, YAMLFactory yamlFactory, KeyStore keystore, char[] keypass, JsonFactory jsonFactory) {
        this.pipelineClient = pipelineClient;
        this.yamlFactory = yamlFactory;
        this.keystore = keystore;
        this.keypass = keypass;
        this.jsonFactory = jsonFactory;
    }

    @Override
    public JobProcessor createFor(Job job) {
        if(job.name().equals("github-pipeline")) {
            return new PipelineJobProcessor(job, new GithubPipelineContextProvider(this.pipelineClient, this.yamlFactory), this::shellExecutor, this.pipelineClient);
        }
        return null;
    }

    private PipelineExecutor shellExecutor(PipelineContext context) {
        return new PipelineShellExecutor(context, this.keystore, this.keypass, this.jsonFactory);
    }

}
