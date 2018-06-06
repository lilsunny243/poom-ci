package org.codingmatters.poom.ci.pipeline.api.service.repository;

import org.codingmatters.poom.ci.pipeline.api.service.repository.impl.InMemoryPoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStageQuery;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLogQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.ci.triggers.UpstreamBuild;
import org.codingmatters.poom.services.domain.repositories.Repository;

import java.util.Objects;

public interface PoomCIRepository {

    static PoomCIRepository inMemory(SegmentedRepository<StageLogKey, StageLog, StageLogQuery> logRepository) {
        return new InMemoryPoomCIRepository(logRepository);
    }

    Repository<Pipeline, String> pipelineRepository();
    Repository<GithubPushEvent, String> githubPushEventRepository();
    Repository<PipelineStage, PipelineStageQuery> stageRepository();
    SegmentedRepository<StageLogKey, StageLog, StageLogQuery> logRepository();
    Repository<UpstreamBuild, String> upstreamBuildRepository();

    class StageLogKey implements SegmentedRepository.Key {
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
