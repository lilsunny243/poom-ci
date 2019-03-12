package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.service.repository.LogFileStore;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.ci.pipeline.api.types.StageStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class AbstractPoomCITest {

    @Rule
    public TemporaryFolder logStorage = new TemporaryFolder();

    private PoomCIRepository inMemory;

    @Before
    public void setUp() throws Exception {
        this.inMemory = PoomCIRepository.inMemory(new LogFileStore(this.logStorage.getRoot()));
    }

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
