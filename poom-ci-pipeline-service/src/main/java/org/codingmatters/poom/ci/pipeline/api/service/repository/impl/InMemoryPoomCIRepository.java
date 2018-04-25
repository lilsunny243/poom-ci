package org.codingmatters.poom.ci.pipeline.api.service.repository.impl;

import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStageQuery;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLogQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.StageStatus;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.stream.Stream;

public class InMemoryPoomCIRepository implements PoomCIRepository {

    private InMemoryRepository<Pipeline, String> pipelineRepository = new InMemoryRepository<Pipeline, String>() {
        @Override
        public PagedEntityList<Pipeline> search(String query, long startIndex, long endIndex) throws RepositoryException {
            return null;
        }
    };

    private InMemoryRepository<GithubPushEvent, String> githubPushEventRepository = new InMemoryRepository<GithubPushEvent, String>() {
        @Override
        public PagedEntityList<GithubPushEvent> search(String query, long startIndex, long endIndex) throws RepositoryException {
            return null;
        }
    };

    private InMemoryRepository<PipelineStage, PipelineStageQuery> stageRepository = new InMemoryRepository<PipelineStage, PipelineStageQuery>() {
        @Override
        public PagedEntityList<PipelineStage> search(PipelineStageQuery query, long startIndex, long endIndex) throws RepositoryException {
            Stream<Entity<PipelineStage>> filtered = this.stream();
            if(query.opt().withPipelineId().isPresent()) {
                filtered = filtered.filter(entity -> query.withPipelineId().equals(entity.value().pipelineId()));
            }
            if(query.opt().withName().isPresent()) {
                filtered = filtered.filter(entity -> query.withName().equals(entity.value().stage().name()));
            }
            if(query.opt().withType().isPresent()) {
                filtered = filtered.filter(entity ->
                        entity.value().opt().stage().stageType().isPresent() &&
                                entity.value().stage().stageType().name().toUpperCase().equals(query.withType().toUpperCase()));
            }

            if(query.opt().withRunningStatus().isPresent()) {
                StageStatus.Run runStatus = StageStatus.Run.valueOf(query.withRunningStatus().toString());
                filtered = filtered.filter(entity -> runStatus.equals(entity.value().stage().status().run()));
            }
            return this.paged(filtered, startIndex, endIndex);
        }
    };
    private Repository<StageLog, StageLogQuery> logRepository = new InMemoryRepository<StageLog, StageLogQuery>() {
        @Override
        public PagedEntityList<StageLog> search(StageLogQuery query, long startIndex, long endIndex) throws RepositoryException {
            Stream<Entity<StageLog>> filtered = this.stream();
            if(query.opt().withPipelineId().isPresent()) {
                filtered = filtered.filter(entity -> query.withPipelineId().equals(entity.value().pipelineId()));
            }
            if(query.opt().withStageName().isPresent()) {
                filtered = filtered.filter(entity -> query.withStageName().equals(entity.value().stageName()));
            }
            return this.paged(filtered, startIndex, endIndex);
        }
    };

    @Override
    public Repository<Pipeline, String> pipelineRepository() {
        return pipelineRepository;
    }

    @Override
    public Repository<GithubPushEvent, String> githubPushEventRepository() {
        return githubPushEventRepository;
    }

    @Override
    public Repository<PipelineStage, PipelineStageQuery> stageRepository() {
        return stageRepository;
    }

    @Override
    public Repository<StageLog, StageLogQuery> logRepository() {
        return this.logRepository;
    }
}
