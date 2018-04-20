package org.codingmatters.poom.ci.pipeline.api.service;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.pipeline.api.PipelinesPostRequest;
import org.codingmatters.poom.ci.pipeline.api.PoomCIPipelineAPIHandlers;
import org.codingmatters.poom.ci.pipeline.api.service.handlers.*;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.JobCollectionPostResponse;
import org.codingmatters.rest.api.Processor;

import java.io.IOException;

public class PoomCIApi {

    static private CategorizedLogger log = CategorizedLogger.getLogger(PoomCIApi.class);

    private final PoomCIRepository repository;
    private final String apiPath;
    private final JsonFactory jsonFactory;
    private final PoomjobsJobRegistryAPIClient jobRegistryAPIClient;

    private PoomCIPipelineAPIHandlers handlers;
    private PoomCIPipelineAPIProcessor processor;


    public PoomCIApi(PoomCIRepository repository, String apiPath, JsonFactory jsonFactory, PoomjobsJobRegistryAPIClient jobRegistryAPIClient) {
        this.repository = repository;
        this.apiPath = apiPath;
        this.jsonFactory = jsonFactory;
        this.jobRegistryAPIClient = jobRegistryAPIClient;

        this.handlers = new PoomCIPipelineAPIHandlers.Builder()
                .githubTriggersPostHandler(new GithubTriggerCreation(this.repository, this::triggerCreated))
                .githubTriggersGetHandler(new GithubTriggersBrowsing(this.repository))
                .githubTriggerGetHandler(new GithubTriggerGet(this.repository))

                .pipelinesGetHandler(new PipelinesBrowsing(this.repository))
                .pipelinesPostHandler(new PipelineCreate(this.repository, this::pipelineCreated))

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


    private void pipelineCreated(Pipeline pipeline) {
        try {
            JobCollectionPostResponse response = this.jobRegistryAPIClient.jobCollection().post(req -> req
                    .accountId("poom-ci")
                    .payload(jobCreation -> jobCreation
                            .category("poom-ci")
                            .name("github-pipeline")
                            .arguments(pipeline.id())
                    )
            );
            if(response.opt().status201().isPresent()) {
                log.audit().info("successfully created job for pipeline {}", pipeline.id());
            } else {
                log.error("error while posting job for pipeline {} : {}", pipeline.id(), response);
            }
        } catch (IOException e) {
            log.error(String.format("error while calling job registry for pipeline %s", pipeline.id()), e);
        }
    }
    private void triggerCreated(PipelineTrigger pipelineTrigger) {
        this.handlers.pipelinesPostHandler().apply(PipelinesPostRequest.builder()
                .payload(pipelineTrigger)
                .build());
    }

    public PoomCIPipelineAPIHandlers handlers() {
        return this.handlers;
    }

    public Processor processor() {
        return this.processor;
    }

}
