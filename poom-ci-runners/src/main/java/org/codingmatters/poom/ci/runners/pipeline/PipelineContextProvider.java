package org.codingmatters.poom.ci.runners.pipeline;

import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;

import java.io.IOException;

@FunctionalInterface
public interface PipelineContextProvider {
    PipelineContext pipelineContext(String pipelineId, PipelineTrigger trigger) throws IOException;
}
