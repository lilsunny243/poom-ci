package org.codingmatters.poom.ci.pipeline.api.service;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.pipeline.api.PoomCIPipelineAPIHandlers;
import org.codingmatters.poom.ci.pipeline.api.service.handlers.*;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.rest.api.Processor;

public class PoomCIApi {

    private final PoomCIRepository repository;
    private final String apiPath;
    private final JsonFactory jsonFactory;

    private PoomCIPipelineAPIHandlers handlers;
    private PoomCIPipelineAPIProcessor processor;

    public PoomCIApi(PoomCIRepository repository, String apiPath, JsonFactory jsonFactory) {
        this.repository = repository;
        this.apiPath = apiPath;
        this.jsonFactory = jsonFactory;

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

        this.processor = new PoomCIPipelineAPIProcessor(this.apiPath, this.jsonFactory, this.handlers);
    }

    public PoomCIPipelineAPIHandlers handlers() {
        return this.handlers;
    }

    public Processor processor() {
        return this.processor;
    }

}
