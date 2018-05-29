package org.codingmatters.poom.ci.runners.pipeline;

import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.pipeline.descriptors.Stage;
import org.codingmatters.poom.ci.pipeline.descriptors.StageHolder;
import org.codingmatters.poom.ci.pipeline.descriptors.ValueList;
import org.codingmatters.poom.ci.pipeline.stage.onlywhen.OnlyWhenVariableProvider;

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
    private final File sources;
    private final String repository;
    private final String branch;
    private final String changeset;

    private final OnlyWhenVariableProvider variableProvider = new OnlyWhenVariableProvider() {
        @Override
        public String branch() {
            return branch;
        }
    };

    public PipelineContext(String pipelineId, Pipeline pipeline, File workspace, File sources, String repository, String branch, String changeset) {
        this.pipelineId = pipelineId;
        this.pipeline = pipeline;
        this.workspace = workspace;
        this.sources = sources;
        this.repository = repository;
        this.branch = branch;
        this.changeset = changeset;
    }

    public String repository() {
        return this.repository;
    }

    public String pipelineId() {
        return pipelineId;
    }

    public String changeset() {
        return changeset;
    }

    public String branch() {
        return branch;
    }

    public Pipeline pipeline() {
        return this.pipeline;
    }

    public File workspace() {
        return this.workspace;
    }

    public File sources() {
        return sources;
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

    public StageHolder[] cleanupStages() {
        return this.stageHolders(this.pipeline.onSuccess(), StageHolder.Type.CLEANUP);
    }

    public StageHolder holder(StageHolder.Type type, String stage) {
        return this.pipeline().holder(StageHolder.Type.MAIN, this.pipeline().stage("stage1"));
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

    public OnlyWhenVariableProvider variableProvider() {
        return this.variableProvider;
    }
}
