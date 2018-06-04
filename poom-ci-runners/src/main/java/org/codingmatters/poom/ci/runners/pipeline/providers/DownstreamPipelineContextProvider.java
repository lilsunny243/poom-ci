package org.codingmatters.poom.ci.runners.pipeline.providers;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.runners.pipeline.PipelineContext;
import org.codingmatters.poom.ci.runners.pipeline.PipelineVariables;
import org.codingmatters.poom.ci.runners.pipeline.providers.downstream.DownstreamPipelineContextVariables;
import org.codingmatters.poom.ci.runners.pipeline.providers.gh.AbstractGitHubPipelineContextProvider;

public class DownstreamPipelineContextProvider extends AbstractGitHubPipelineContextProvider implements PipelineContext.PipelineContextProvider {
    public DownstreamPipelineContextProvider(PoomCIPipelineAPIClient pipelineClient, YAMLFactory yamlFactory) {
        super(pipelineClient, yamlFactory);
    }

    @Override
    protected PipelineVariables createVariables(String pipelineId, PipelineTrigger trigger) throws ProcessingException {
        return new DownstreamPipelineContextVariables(pipelineId, trigger, this.pipelineAPIClient()).variables();
    }
}
