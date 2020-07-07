package org.codingmatters.poom.ci.pipeline.api.service.repository;

import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLogQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.services.domain.repositories.EntityLister;

import java.io.IOException;

public interface LogStore {

    Segment segment(String pipelineId, Stage.StageType stageType, String stageName);

    interface Segment extends EntityLister<StageLog, StageLogQuery> {
        void append(String ... lines) throws IOException;
    }
}
