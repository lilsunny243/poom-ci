package org.codingmatters.poom.ci.runners.pipeline;

import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.pipeline.descriptors.Stage;
import org.codingmatters.poom.ci.pipeline.descriptors.StageHolder;
import org.codingmatters.poom.ci.pipeline.descriptors.ValueList;

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

    public StageHolder[] stages() {
        return this.stageHolders(this.pipeline.stages(), StageHolder.Type.MAIN);
    }

    public StageHolder[] onErrorStages() {
        return this.stageHolders(this.pipeline.onError(), StageHolder.Type.ERROR);
    }

    public StageHolder[] onSuccessStages() {
        return this.stageHolders(this.pipeline.onSuccess(), StageHolder.Type.SUCCESS);
    }

    private StageHolder[] stageHolders(ValueList<Stage> stages, StageHolder.Type type) {
        return stages == null ? new StageHolder[0] :
                stages.stream()
                .map(stage -> StageHolder.builder()
                        .stage(stage)
                        .type(type)
                        .build())
                .toArray(i -> new StageHolder[i]);
    }
}
