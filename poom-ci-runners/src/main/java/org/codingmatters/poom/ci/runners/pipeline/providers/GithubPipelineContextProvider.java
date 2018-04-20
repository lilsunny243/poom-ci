package org.codingmatters.poom.ci.runners.pipeline.providers;

import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.runners.pipeline.PipelineContext;

import java.io.IOException;

public class GithubPipelineContextProvider implements PipelineContext.PipelineContextProvider {
    @Override
    public PipelineContext pipelineContext(String pipelineId, PipelineTrigger trigger) throws IOException {
        return null;
    }
}
