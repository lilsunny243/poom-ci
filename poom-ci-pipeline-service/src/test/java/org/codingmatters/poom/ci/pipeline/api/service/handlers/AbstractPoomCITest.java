package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.repository.logs.RepositoryLogStore;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.ci.pipeline.api.types.StageStatus;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.ci.triggers.UpstreamBuild;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class AbstractPoomCITest {

    @Rule
    public TemporaryFolder logStorage = new TemporaryFolder();

    private PoomCIRepository inMemory;

    @Before
    public void setUp() throws Exception {
        this.inMemory = new PoomCIRepository(
                new RepositoryLogStore(InMemoryRepositoryWithPropertyQuery.validating(StageLog.class)),
                InMemoryRepositoryWithPropertyQuery.validating(Pipeline.class),
                InMemoryRepositoryWithPropertyQuery.validating(GithubPushEvent.class),
                InMemoryRepositoryWithPropertyQuery.validating(UpstreamBuild.class),
                InMemoryRepositoryWithPropertyQuery.validating(PipelineStage.class)
        );
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
