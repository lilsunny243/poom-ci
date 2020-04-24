package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineStageGetRequest;
import org.codingmatters.poom.ci.pipeline.api.pipelinestagegetresponse.Status200;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.ci.pipeline.api.types.StageStatus;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StageGetTest extends AbstractPoomCITest {

    private StageGet handler;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        handler = new StageGet(this.repository());
        for (int i = 0; i < 500; i++) {
            String id = "" + i;
            this.repository().stageRepository().create(PipelineStage.builder()
                    .pipelineId("pipe-" + id)
                    .stage(stage -> stage.name("stage-" + id).stageType(Stage.StageType.MAIN))
                    .build());
        }
    }

    @Test
    public void whenNoPipeline__then404() {
        this.handler.apply(PipelineStageGetRequest.builder()
                .stageName("yop").build())
                .opt().status404().orElseThrow(() -> new AssertionError("should have a 404"));
    }

    @Test
    public void whenNostage__then404() {
        this.handler.apply(PipelineStageGetRequest.builder()
                .pipelineId("12").build())
                .opt().status404().orElseThrow(() -> new AssertionError("should have a 404"));
    }

    @Test
    public void notFound() {
        this.handler.apply(PipelineStageGetRequest.builder()
                .pipelineId("12").stageName("yop").build())
                .opt().status404().orElseThrow(() -> new AssertionError("should have a 404"));
    }

    @Test
    public void found() throws Exception {
        this.repository().stageRepository().create(PipelineStage.builder()
                .pipelineId("12")
                .stage(stage -> stage
                        .name("my-stage")
                        .stageType(Stage.StageType.ERROR)
                        .status(status -> status
                                .run(StageStatus.Run.DONE)
                                .exit(StageStatus.Exit.SUCCESS)))
                .build());

        Entity<PipelineStage> storedStage = this.repository().stageRepository().create(PipelineStage.builder()
                .pipelineId("12")
                .stage(stage -> stage
                        .name("my-stage")
                        .stageType(Stage.StageType.MAIN)
                        .status(status -> status
                                .run(StageStatus.Run.DONE)
                                .exit(StageStatus.Exit.SUCCESS)))
                .build());


        Status200 response = this.handler.apply(PipelineStageGetRequest.builder()
                .pipelineId("12")
                .stageName(storedStage.value().stage().name())
                .stageType("main")
                .build())
                .opt().status200().orElseThrow(() -> new AssertionError("should have a 200"));

        assertThat(response.xEntityId(), is(storedStage.value().stage().name()));
        assertThat(response.xPipelineId(), is("12"));
        assertThat(response.payload(), is(storedStage.value().stage()));
    }
}