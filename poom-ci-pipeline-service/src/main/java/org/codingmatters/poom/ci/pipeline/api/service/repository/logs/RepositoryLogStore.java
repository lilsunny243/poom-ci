package org.codingmatters.poom.ci.pipeline.api.service.repository.logs;

import org.codingmatters.poom.ci.pipeline.api.service.repository.LogStore;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLogQuery;
import org.codingmatters.poom.ci.pipeline.api.types.LogLine;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.io.IOException;
import java.util.List;

public class RepositoryLogStore implements LogStore {
    private final Repository<StageLog, PropertyQuery> repository;

    public RepositoryLogStore(Repository<StageLog, PropertyQuery> repository) {
        this.repository = repository;
    }

    @Override
    public Segment segment(String pipelineId, Stage.StageType stageType, String stageName) {
        return new RepositorySegment(this.repository, pipelineId, stageType, stageName);
    }

    class RepositorySegment implements Segment {

        private final Repository<StageLog, PropertyQuery> repository;
        private final String pipelineId;
        private final Stage.StageType stageType;
        private final String stageName;

        public RepositorySegment(Repository<StageLog, PropertyQuery> repository, String pipelineId, Stage.StageType stageType, String stageName) {
            this.repository = repository;
            this.pipelineId = pipelineId;
            this.stageType = stageType;
            this.stageName = stageName;
        }

        @Override
        public void append(String... lines) throws IOException {
            Long index;
            try {
                List<StageLog> lastLogs = this.repository.search(PropertyQuery.builder()
                        .filter(String.format(
                        "pipelineId == '%s' && stageType == '%s' && stageName == '%s'",
                        this.pipelineId, this.stageType.name(), this.stageName
                        ))
                        .sort("log.line desc")
                        .build(), 0, 0).valueList();
                if(! lastLogs.isEmpty()) {
                    index = lastLogs.get(0).log().line();
                } else {
                    index = 0L;
                }
            } catch (RepositoryException e) {
                throw new IOException("failed getting last log", e);
            }
            for (String line : lines) {
                try {
                    index ++;
                    this.repository.create(StageLog.builder()
                            .pipelineId(this.pipelineId)
                            .stageName(this.stageName)
                            .stageType(this.stageType)
                            .when(UTC.now())
                            .log(LogLine.builder()
                                    .content(line)
                                    .line(index)
                                    .build())
                            .build());
                } catch (RepositoryException e) {
                    throw new IOException("failed adding log line", e);
                }
            }

        }

        @Override
        public PagedEntityList<StageLog> all(long startIndex, long endIndex) throws RepositoryException {
            return this.repository.search(PropertyQuery.builder()
                            .filter(this.segmentFilter())
                            .sort("log.line asc")
                            .build(),
                    startIndex, endIndex);
        }

        @Override
        public PagedEntityList<StageLog> search(StageLogQuery query, long startIndex, long endIndex) throws RepositoryException {
            return this.repository.search(PropertyQuery.builder()
                    .filter(this.segmentFilter())
                    .sort("log.line asc")
                    .build(),
                    startIndex, endIndex);
        }

        public String segmentFilter() {
            return String.format(
                    "pipelineId == '%s' && stageName == '%s' && stageType == '%s'",
                    this.pipelineId,
                    this.stageName,
                    this.stageType
            );
        }
    }
}
