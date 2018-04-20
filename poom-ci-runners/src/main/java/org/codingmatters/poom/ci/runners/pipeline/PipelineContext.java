package org.codingmatters.poom.ci.runners.pipeline;

import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;

import java.io.File;
import java.io.IOException;

public class PipelineContext {

    @FunctionalInterface
    public interface PipelineContextProvider {
        PipelineContext pipelineContext(String pipelineId, PipelineTrigger trigger) throws IOException;
    }

    private final String pipelineId;
    private final Pipeline pipeline;
    private final File workspace;


    public PipelineContext(String pipelineId, Pipeline pipeline, File workspace) {
        this.pipelineId = pipelineId;
        this.pipeline = pipeline;
        this.workspace = workspace;
    }

    public String pipelineId() {
        return pipelineId;
    }

    public Pipeline pipeline() {
        return this.pipeline;
    }

    public File workspace() {
        return this.workspace;
    }

    public String[] stages() {
        return this.pipeline().stages().stream()
                .map(stage -> stage.name())
                .toArray(i -> new String[i]);
    }
}
