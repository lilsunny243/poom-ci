package org.codingmatters.poom.ci.pipeline.api.service.repository.impl;

import org.codingmatters.poom.ci.pipeline.api.service.repository.LogFileStore;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineQuery;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStageQuery;
import org.codingmatters.poom.ci.pipeline.api.service.storage.UpstreamBuildQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.StageStatus;
import org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.ci.triggers.UpstreamBuild;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.stream.Stream;

public class InMemoryPoomCIRepository implements PoomCIRepository {

    private final Repository<Pipeline, PropertyQuery> pipelineRepository = InMemoryRepositoryWithPropertyQuery.validating(Pipeline.class);
    private final Repository<GithubPushEvent, PropertyQuery> githubPushEventRepository = InMemoryRepositoryWithPropertyQuery.validating(GithubPushEvent.class);
    private final Repository<UpstreamBuild, PropertyQuery> upstreamBuildRepository = InMemoryRepositoryWithPropertyQuery.validating(UpstreamBuild.class);
    private final Repository<PipelineStage, PropertyQuery> stageRepository = InMemoryRepositoryWithPropertyQuery.validating(PipelineStage.class);

    private final LogFileStore logStore;

    public InMemoryPoomCIRepository(LogFileStore logStore) {
        this.logStore = logStore;
    }


    @Override
    public Repository<Pipeline, PropertyQuery> pipelineRepository() {
        return pipelineRepository;
    }

    @Override
    public Repository<GithubPushEvent, PropertyQuery> githubPushEventRepository() {
        return githubPushEventRepository;
    }

    @Override
    public Repository<PipelineStage, PropertyQuery> stageRepository() {
        return stageRepository;
    }

    @Override
    public LogFileStore logStore() {
        return logStore;
    }

    @Override
    public Repository<UpstreamBuild, PropertyQuery> upstreamBuildRepository() {
        return this.upstreamBuildRepository;
    }
}
