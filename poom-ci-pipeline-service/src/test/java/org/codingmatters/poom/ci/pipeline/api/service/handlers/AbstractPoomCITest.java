package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.repository.SegmentedRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLogQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.ci.pipeline.api.types.StageStatus;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepository;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.HashMap;
import java.util.Map;

public class AbstractPoomCITest {

    private PoomCIRepository inMemory = PoomCIRepository.inMemory(
            new SegmentedRepository<PoomCIRepository.StageLogKey, StageLog, StageLogQuery>() {
                private final Map<PoomCIRepository.StageLogKey, Repository<StageLog, StageLogQuery>> storage = new HashMap<>();

                @Override
                public synchronized Repository<StageLog, StageLogQuery> repository(PoomCIRepository.StageLogKey key) {
                    this.storage.computeIfAbsent(key,  this::createStageLogRepository);
                    return this.storage.get(key);
                }

                private Repository<StageLog, StageLogQuery> createStageLogRepository(PoomCIRepository.StageLogKey key) {
                    return new InMemoryRepository<StageLog, StageLogQuery>() {
                        @Override
                        public PagedEntityList<StageLog> search(StageLogQuery query, long startIndex, long endIndex) throws RepositoryException {
                            throw new UnsupportedOperationException();
                        }
                    };
                }

            }
    );

    public PoomCIRepository repository() {
        return inMemory;
    }


    protected void createSomeStages(String pipelineId) throws org.codingmatters.poom.services.domain.exceptions.RepositoryException {
        for (int i = 0; i < 300; i++) {
            Stage.StageType stageType = Stage.StageType.MAIN;
            switch (i % 3) {
                case 0:
                    stageType = Stage.StageType.MAIN;
                    break;
                case 1:
                    stageType = Stage.StageType.SUCCESS;
                    break;
                case 2:
                    stageType = Stage.StageType.ERROR;
                    break;
            }
            this.repository().stageRepository().create(PipelineStage.builder()
                    .pipelineId(pipelineId)
                    .stage(Stage.builder()
                            .name("stage-" + i)
                            .stageType(stageType)
                            .status(status -> status.run(StageStatus.Run.DONE).exit(StageStatus.Exit.SUCCESS))
                            .build())
                    .build());
        }
    }
}
