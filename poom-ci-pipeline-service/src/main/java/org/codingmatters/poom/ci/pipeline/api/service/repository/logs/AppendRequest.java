package org.codingmatters.poom.ci.pipeline.api.service.repository.logs;

import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.types.LogLine;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.support.date.UTC;

import java.io.IOException;
import java.util.List;

public class AppendRequest {
    private final String pipelineId;
    private final Stage.StageType stageType;
    private final String stageName;
    private final String[] lines;

    public AppendRequest(String pipelineId, Stage.StageType stageType, String stageName, String[] lines) {
        this.pipelineId = pipelineId;
        this.stageType = stageType;
        this.stageName = stageName;
        this.lines = lines;
    }

    public void appendTo(Repository<StageLog, PropertyQuery> repository) throws IOException {
        Long index;
        try {
            List<StageLog> lastLogs = repository.search(PropertyQuery.builder()
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
                repository.create(StageLog.builder()
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
}
