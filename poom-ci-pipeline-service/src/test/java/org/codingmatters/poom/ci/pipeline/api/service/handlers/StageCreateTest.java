package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineStagesPostRequest;
import org.codingmatters.poom.ci.pipeline.api.pipelinestagespostresponse.Status201;
import org.codingmatters.poom.ci.pipeline.api.pipelinestagespostresponse.Status400;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.StageStatus;
import org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StageCreateTest extends AbstractPoomCITest {

    private StageCreate handler = new StageCreate(this.repository());
    private String runningPipelineId;
    private String donePipelineId;

    @Before
    public void setUp() throws Exception {
        Entity<Pipeline> pipeline = this.repository().pipelineRepository().create(Pipeline.builder()
                .status(status -> status.run(Status.Run.RUNNING))
                .build());
        this.repository().pipelineRepository().update(pipeline, pipeline.value().withId(pipeline.id()));
        this.runningPipelineId = pipeline.id();

        pipeline = this.repository().pipelineRepository().create(Pipeline.builder()
                .status(status -> status.run(Status.Run.DONE).exit(Status.Exit.SUCCESS))
                .build());
        this.repository().pipelineRepository().update(pipeline, pipeline.value().withId(pipeline.id()));
        this.donePipelineId = pipeline.id();
    }

    @Test
    public void givenPipelineIsRunning__whenAStageIsPosted__thenStageIsCreated_andStageStatusIsRunning() throws Exception {
        Status201 response = this.handler.apply(PipelineStagesPostRequest.builder()
                .pipelineId(this.runningPipelineId)
                .payload(creation -> creation.name("a-stage"))
                .build())
                .opt().status201()
                .orElseThrow(() -> new AssertionError("should have a 201"));

        assertThat(response.xEntityId(), is("a-stage"));
        assertThat(response.location(), is("%API_PATH%/pipelines/" + this.runningPipelineId + "/stages/a-stage"));

        Entity<PipelineStage> created = this.repository().stageRepository().all(0, 0).get(0);
        assertThat(
                created.value(),
                is(PipelineStage.builder()
                        .pipelineId(this.runningPipelineId)
                        .stage(stage -> stage.name("a-stage").status(status -> status.run(StageStatus.Run.RUNNING).exit(null)))
                        .build())
        );
    }

    @Test
    public void givenPipelineDoesntExists__whenStageIsPosted__thenRequestIsNotAcceptable() throws Exception {
        Status400 response = this.handler.apply(PipelineStagesPostRequest.builder()
                .pipelineId("no-such-pipeline")
                .payload(creation -> creation.name("a-stage"))
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));

        assertThat(response.payload().code(), is(Error.Code.ILLEGAL_RESOURCE_CREATION));
        assertThat(response.payload().description(), is("cannot create stage for unexistent pipeline"));
    }

    @Test
    public void givenPipelineIsNotRunning__whenStageIsPosted__thenRequestIsNotAcceptable() throws Exception {
        Status400 response = this.handler.apply(PipelineStagesPostRequest.builder()
                .pipelineId(this.donePipelineId)
                .payload(creation -> creation.name("a-stage"))
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));

        assertThat(response.payload().code(), is(Error.Code.ILLEGAL_RESOURCE_CREATION));
        assertThat(response.payload().description(), is("cannot create stage for a pipeline that is not running"));
    }

    @Test
    public void givenStageAlreadyExists__whenStageIsPosted__thenRequestIsNotAcceptable() throws Exception {
        this.repository().stageRepository().create(PipelineStage.builder()
                .pipelineId(this.runningPipelineId)
                .stage(stage -> stage.name("a-stage").status(status -> status.run(StageStatus.Run.RUNNING)))
                .build());

        Status400 response = this.handler.apply(PipelineStagesPostRequest.builder()
                .pipelineId(this.runningPipelineId)
                .payload(creation -> creation.name("a-stage"))
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));

        assertThat(response.payload().code(), is(Error.Code.ILLEGAL_RESOURCE_CREATION));
        assertThat(response.payload().description(), is("state already exists"));
    }

    @Test
    public void givenAnotherStageIs__whenStageIsPosted__thenRequestIsNotAcceptable() throws Exception {
        this.repository().stageRepository().create(PipelineStage.builder()
                .pipelineId(this.runningPipelineId)
                .stage(stage -> stage.name("another-stage").status(status -> status.run(StageStatus.Run.RUNNING)))
                .build());

        Status400 response = this.handler.apply(PipelineStagesPostRequest.builder()
                .pipelineId(this.runningPipelineId)
                .payload(creation -> creation.name("a-stage"))
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));

        assertThat(response.payload().code(), is(Error.Code.ILLEGAL_RESOURCE_CREATION));
        assertThat(response.payload().description(), is("there's already a running stage"));
    }
}