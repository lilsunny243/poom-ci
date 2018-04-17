package org.codingmatters.poom.ci.pipeline.api.service.repository;

import org.codingmatters.poom.ci.pipeline.api.service.repository.impl.InMemoryPoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStageQuery;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLogQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.services.domain.repositories.Repository;

public interface PoomCIRepository {

    static PoomCIRepository inMemory() {
        return new InMemoryPoomCIRepository();
    }

    Repository<Pipeline, String> pipelineRepository();
    Repository<GithubPushEvent, String> githubPushEventRepository();
    Repository<PipelineStage, PipelineStageQuery> stageRepository();
    Repository<StageLog, StageLogQuery> logRepository();
}
