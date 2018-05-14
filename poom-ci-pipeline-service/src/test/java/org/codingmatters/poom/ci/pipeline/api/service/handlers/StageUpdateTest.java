package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineStagePatchRequest;
import org.codingmatters.poom.ci.pipeline.api.pipelinestagepatchresponse.Status200;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.ci.pipeline.api.types.StageStatus;
import org.codingmatters.poom.ci.pipeline.api.types.StageTermination;
import org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StageUpdateTest extends AbstractPoomCITest {

    private StageUpdate handler = new StageUpdate(this.repository());

    private String runningPipelineId;
    private Entity<PipelineStage> existingStage;

    @Before
    public void setUp() throws Exception {
        Entity<Pipeline> pipeline = this.repository().pipelineRepository().create(Pipeline.builder()
                .status(status -> status.run(Status.Run.RUNNING))
                .build());
        this.repository().pipelineRepository().update(pipeline, pipeline.value().withId(pipeline.id()));
        this.runningPipelineId = pipeline.id();

        this.existingStage = this.repository().stageRepository().create(PipelineStage.builder()
                .pipelineId(this.runningPipelineId)
                .stage(stage -> stage
                        .name("a-stage")
                        .stageType(Stage.StageType.ERROR)
                        .status(status -> status.run(StageStatus.Run.RUNNING))
                )
                .build());
        this.existingStage = this.repository().stageRepository().create(PipelineStage.builder()
                .pipelineId(this.runningPipelineId)
                .stage(stage -> stage
                        .name("a-stage")
                        .stageType(Stage.StageType.MAIN)
                        .status(status -> status.run(StageStatus.Run.RUNNING))
                )
                .build());
    }

    @Test
    public void whenNoStageType__thenRequestIsInvalid() {
        this.handler.apply(PipelineStagePatchRequest.builder()
                .pipelineId(this.runningPipelineId)
                .stageName(this.existingStage.value().stage().name())
                .payload(status -> status
                        .exit(StageTermination.Exit.SUCCESS))
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));
    }

    @Test
    public void whenInvalidStageType__thenRequestIsInvalid() {
        this.handler.apply(PipelineStagePatchRequest.builder()
                .pipelineId(this.runningPipelineId)
                .stageName(this.existingStage.value().stage().name())
                .stageType("no such stage type")
                .payload(status -> status
                        .exit(StageTermination.Exit.SUCCESS))
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));
    }

    @Test
    public void givenStageExists__whenStatusChangeIsPatched__thenEntityIsModifiedAndRetrurn() throws Exception {
        Status200 response = this.handler.apply(PipelineStagePatchRequest.builder()
                .pipelineId(this.runningPipelineId)
                .stageName(this.existingStage.value().stage().name())
                .stageType("main")
                .payload(status -> status
                        .exit(StageTermination.Exit.SUCCESS))
                .build())
                .opt().status200()
                .orElseThrow(() -> new AssertionError("should have a 200"));

        Entity<PipelineStage> updated = this.repository().stageRepository().retrieve(this.existingStage.id());

        assertThat(
                updated.value().stage(),
                is(this.existingStage.value().stage().withStatus(StageStatus.builder()
                        .run(StageStatus.Run.DONE)
                        .exit(StageStatus.Exit.SUCCESS)
                        .build()
                ))
        );

        assertThat(
                response.payload(),
                is(updated.value().stage())
        );
    }

    @Test
    public void givenStageDoesntExist__whenStatusChangeIsPatched__thenResourceNotFound() {
        this.handler.apply(PipelineStagePatchRequest.builder()
                .pipelineId(this.runningPipelineId)
                .stageName("no-such-stage")
                .stageType("main")
                .payload(status -> status
                        .exit(StageTermination.Exit.SUCCESS))
                .build())
                .opt().status404()
                .orElseThrow(() -> new AssertionError("should have a 404"));
    }

    @Test
    public void givenPipelineDoesntExist__whenStatusChangeIsPatched__thenResourceNotFound() {
        this.handler.apply(PipelineStagePatchRequest.builder()
                .pipelineId("no-such-pipeline")
                .stageName(this.existingStage.value().stage().name())
                .stageType("main")
                .payload(status -> status
                        .exit(StageTermination.Exit.SUCCESS))
                .build())
                .opt().status404()
                .orElseThrow(() -> new AssertionError("should have a 404"));
    }

    @Test
    public void givenStageIsAlreadyDone__whenStatusChangeIsPatched__thenIllegalResourceChange() throws Exception {
        this.repository().stageRepository().update(this.existingStage, this.existingStage.value()
                .withStage(this.existingStage.value().stage()
                        .withStatus(StageStatus.builder().run(StageStatus.Run.DONE).build())
                ));

        this.handler.apply(PipelineStagePatchRequest.builder()
                .pipelineId(this.runningPipelineId)
                .stageName(this.existingStage.value().stage().name())
                .stageType("main")
                .payload(status -> status
                        .exit(StageTermination.Exit.SUCCESS))
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));
    }
}