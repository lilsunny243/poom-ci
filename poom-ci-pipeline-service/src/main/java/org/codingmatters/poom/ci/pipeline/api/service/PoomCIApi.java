package org.codingmatters.poom.ci.pipeline.api.service;

import org.codingmatters.poom.ci.pipeline.api.PoomCIPipelineAPIHandlers;
import org.codingmatters.poom.ci.pipeline.api.service.handlers.*;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;

public class PoomCIApi {

    private final PoomCIRepository repository;

    private PoomCIPipelineAPIHandlers handlers;

    public PoomCIApi(PoomCIRepository repository) {
        this.repository = repository;

        this.handlers = new PoomCIPipelineAPIHandlers.Builder()
                .githubTriggersPostHandler(new GithubTriggerCreation(this.repository))
                .githubTriggersGetHandler(new GithubTriggersBrowsing(this.repository))
                .githubTriggerGetHandler(new GithubTriggerGet(this.repository))

                .pipelinesGetHandler(new PipelinesBrowsing(this.repository))
                .pipelineGetHandler(new PipelineGet(this.repository))
                .pipelinePatchHandler(new PipelineUpdate(this.repository))

                .pipelineStagesGetHandler(new StagesBrowsing(this.repository))
                .pipelineStagesPostHandler(new StageCreate(this.repository))

                .pipelineStageGetHandler(new StageGet(this.repository))
                .pipelineStagePatchHandler(new StageUpdate(this.repository))

                .pipelineStageLogsGetHandler(new StageLogsBrowsing(this.repository))
                .pipelineStageLogsPatchHandler(new AppendStageLogs(this.repository))

                .build();
    }

    public PoomCIPipelineAPIHandlers handlers() {
        return this.handlers;
    }

}
