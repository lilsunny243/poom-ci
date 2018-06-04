package org.codingmatters.poom.ci.runners.pipeline;

import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.pipeline.descriptors.Stage;
import org.codingmatters.poom.ci.pipeline.descriptors.StageHolder;
import org.codingmatters.poom.ci.pipeline.descriptors.ValueList;
import org.codingmatters.poom.ci.pipeline.stage.onlywhen.OnlyWhenVariableProvider;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class PipelineContext {

    @FunctionalInterface
    public interface PipelineContextProvider {
        PipelineContext pipelineContext(String pipelineId, PipelineTrigger trigger) throws IOException;
    }

    private final PipelineVariables variables;
    private final Pipeline pipeline;
    private final File workspace;
    private final File sources;

    private final OnlyWhenVariableProvider variableProvider = new OnlyWhenVariableProvider() {
        @Override
        public String branch() {
            return variableProvider.branch();
        }
    };

    public PipelineContext(PipelineVariables variables, Pipeline pipeline, File workspace, File sources) {
        this.variables = variables;
        this.pipeline = pipeline;
        this.workspace = workspace;
        this.sources = sources;
    }

    public PipelineVariables variables() {
        return variables;
    }



    public void setVariablesTo(Map<String, String> env) {
        env.put("PIPELINE_ID", this.variables.pipelineId());
        env.put("REPOSITORY_ID", this.variables.repositoryId());
        env.put("REPOSITORY", this.variables.repository());
        env.put("REPOSITORY_URL", this.variables.repositoryUrl());
        env.put("CHECKOUT_SPEC", this.variables.checkoutSpec());
        env.put("BRANCH", this.variables.branch());
        env.put("CHANGESET", this.variables.changeset());
    }

    public String repository() {
        return this.variables.repository();
    }

    public String pipelineId() {
        return this.variables.pipelineId();
    }

    public String changeset() {
        return this.variables.changeset();
    }

    public String branch() {
        return this.variables.branch();
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
        return this.stageHolders(this.pipeline.cleanup(), StageHolder.Type.CLEANUP);
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
