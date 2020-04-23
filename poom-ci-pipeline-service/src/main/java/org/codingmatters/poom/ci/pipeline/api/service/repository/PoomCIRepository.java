package org.codingmatters.poom.ci.pipeline.api.service.repository;

import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.ci.triggers.UpstreamBuild;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;

import java.util.Objects;

public class PoomCIRepository {

    private final Repository<Pipeline, PropertyQuery> pipelineRepository;
    private final Repository<GithubPushEvent, PropertyQuery> githubPushEventRepository;
    private final Repository<UpstreamBuild, PropertyQuery> upstreamBuildRepository;
    private final Repository<PipelineStage, PropertyQuery> stageRepository;

    private final LogFileStore logStore;

    public PoomCIRepository(
            LogFileStore logStore,
            Repository<Pipeline, PropertyQuery> pipelineRepository,
            Repository<GithubPushEvent, PropertyQuery> githubPushEventRepository,
            Repository<UpstreamBuild, PropertyQuery> upstreamBuildRepository,
            Repository<PipelineStage, PropertyQuery> stageRepository
    ) {
        this.logStore = logStore;
        this.pipelineRepository = pipelineRepository;
        this.githubPushEventRepository = githubPushEventRepository;
        this.upstreamBuildRepository = upstreamBuildRepository;
        this.stageRepository = stageRepository;
    }


    public Repository<Pipeline, PropertyQuery> pipelineRepository() {
        return pipelineRepository;
    }

    public Repository<GithubPushEvent, PropertyQuery> githubPushEventRepository() {
        return githubPushEventRepository;
    }

    public Repository<PipelineStage, PropertyQuery> stageRepository() {
        return stageRepository;
    }

    public LogFileStore logStore() {
        return logStore;
    }

    public Repository<UpstreamBuild, PropertyQuery> upstreamBuildRepository() {
        return this.upstreamBuildRepository;
    }

    static public class StageLogKey implements SegmentedRepository.Key {
        private final String pipelineId;
        private final Stage.StageType stageType;
        private final String stageName;

        public StageLogKey(String pipelineId, Stage.StageType stageType, String stageName) {
            this.pipelineId = pipelineId;
            this.stageName = stageName;
            this.stageType = stageType;
        }

        @Override
        public String segmentName() {
            return String.format("%s-%s-%s", this.pipelineId, this.stageType.name(), this.stageName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StageLogKey that = (StageLogKey) o;
            return Objects.equals(pipelineId, that.pipelineId) &&
                    stageType == that.stageType &&
                    Objects.equals(stageName, that.stageName);
        }

        @Override
        public int hashCode() {

            return Objects.hash(pipelineId, stageType, stageName);
        }
    }
}
