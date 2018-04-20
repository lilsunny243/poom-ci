package org.codingmatters.poom.ci.runners;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.runners.pipeline.PipelineContext;
import org.codingmatters.poom.ci.runners.pipeline.PipelineExecutor;
import org.codingmatters.poom.ci.runners.pipeline.PipelineJobProcessor;
import org.codingmatters.poom.ci.runners.pipeline.executors.PipelineShellExecutor;
import org.codingmatters.poom.ci.runners.pipeline.providers.GithubPipelineContextProvider;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poomjobs.api.types.Job;

public class PoomCIJobProcessorFactory implements JobProcessor.Factory {

    private PoomCIPipelineAPIClient pipelineClient;
    private YAMLFactory yamlFactory;

    @Override
    public JobProcessor createFor(Job job) {
        if(job.name().equals("github-pipeline")) {
            return new PipelineJobProcessor(job, new GithubPipelineContextProvider(this.pipelineClient, this.yamlFactory), this::shellExecutor, this.pipelineClient);
        }
        return null;
    }

    private PipelineExecutor shellExecutor(PipelineContext context) {
        return new PipelineShellExecutor(context);
    }

}
