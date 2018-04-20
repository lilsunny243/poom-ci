package org.codingmatters.poom.ci.runners.pipeline;

import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;

public class PipelineContext {

    private final String pipelineId;
    private final Pipeline pipeline;

    public PipelineContext(String pipelineId, Pipeline pipeline) {
        this.pipelineId = pipelineId;
        this.pipeline = pipeline;
    }

    public String pipelineId() {
        return pipelineId;
    }

    public Pipeline pipeline() {
        return this.pipeline;
    }


    public String[] stages() {
        return this.pipeline().stages().stream()
                .map(stage -> stage.name())
                .toArray(i -> new String[i]);
    }

}
