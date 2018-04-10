package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PoomCIPipelineAPIHandlers;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.services.domain.repositories.Repository;

public class PoomCIApi {

    private final Repository<GithubPushEvent, String> githubPushEventRepository;
    private final Repository<Pipeline, String> pipelineRepository;
    private PoomCIPipelineAPIHandlers handlers;

    public PoomCIApi(Repository<Pipeline, String> pipelineRepository, Repository<GithubPushEvent, String> githubPushEventRepository) {
        this.pipelineRepository = pipelineRepository;
        this.githubPushEventRepository = githubPushEventRepository;

        this.handlers = new PoomCIPipelineAPIHandlers.Builder()
                .githubTriggersPostHandler(new GithubTriggerCreation(this.githubPushEventRepository, this.pipelineRepository))
                .githubTriggersGetHandler(new GithubTriggersBrowsing(this.githubPushEventRepository))
                .githubTriggerGetHandler(new GithubTriggerGet(this.githubPushEventRepository))

                .pipelinesGetHandler(new PipelinesBrowsing(this.pipelineRepository))
                .pipelineGetHandler(new PipelineGet(this.pipelineRepository))
                .build();
    }

    public PoomCIPipelineAPIHandlers handlers() {
        return this.handlers;
    }

}
