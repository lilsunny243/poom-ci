package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineStagesPostRequest;
import org.codingmatters.poom.ci.pipeline.api.pipelinestagespostresponse.Status201;
import org.codingmatters.poom.ci.pipeline.api.pipelinestagespostresponse.Status400;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.ci.pipeline.api.types.StageStatus;
import org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class StageCreateTest extends AbstractPoomCITest {

    private StageCreate handler;
    private String runningPipelineId;
    private String donePipelineId;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        handler = new StageCreate(this.repository());
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
                .stageType("main")
                .payload(creation -> creation.name("a-stage"))
                .build())
                .opt().status201()
                .orElseThrow(() -> new AssertionError("should have a 201"));

        assertThat(response.xEntityId(), is("a-stage"));
        assertThat(response.location(), is("%API_PATH%/pipelines/" + this.runningPipelineId + "/stages/a-stage"));

        PipelineStage created = this.repository().stageRepository().all(0, 0).get(0).value();

        assertThat(created.pipelineId(), is(this.runningPipelineId));
        assertThat(created.stage().name(), is("a-stage"));
        assertThat(created.stage().stageType(), is(Stage.StageType.MAIN));
        assertThat(created.stage().status().run(), is(StageStatus.Run.RUNNING));
        assertThat(created.stage().status().exit(), is(nullValue()));
        assertThat(created.stage().triggered(), is(notNullValue()));
        assertThat(created.stage().finished(), is(nullValue()));
    }

    @Test
    public void givenPipelineIsRunning__whenTwoStagesArePostedWithSameNameInDifferentStageType__thenStagesAreCreated() throws Exception {
        this.handler.apply(PipelineStagesPostRequest.builder()
                .pipelineId(this.runningPipelineId)
                .stageType("main")
                .payload(creation -> creation.name("a-stage"))
                .build())
                .opt().status201()
                .orElseThrow(() -> new AssertionError("should have a 201"));

        Entity<PipelineStage> first = this.repository().stageRepository().all(0, 0).get(0);

        System.out.println(first);
        this.repository().stageRepository().update(first, first.value().withStage(first.value().stage().withStatus(first.value().stage().status()
                .withRun(StageStatus.Run.DONE)
                .withExit(StageStatus.Exit.SUCCESS))));


        this.handler.apply(PipelineStagesPostRequest.builder()
                .pipelineId(this.runningPipelineId)
                .stageType("error")
                .payload(creation -> creation.name("a-stage"))
                .build())
                .opt().status201()
                .orElseThrow(() -> new AssertionError("should have a 201"));

        Entity<PipelineStage> second = this.repository().stageRepository().all(1, 1).get(0);

        assertThat(second.id(), is(not(first.id())));
        assertThat(first.value().stage().stageType(), is(Stage.StageType.MAIN));
        assertThat(second.value().stage().stageType(), is(Stage.StageType.ERROR));
    }

    @Test
    public void givenPipelineIsRunning__whenAStageIsPostedWithoutStageType__thenRequestIsNotAcceptable() throws Exception {
        this.handler.apply(PipelineStagesPostRequest.builder()
                .pipelineId(this.runningPipelineId)
                .payload(creation -> creation.name("a-stage"))
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));
    }

    @Test
    public void givenPipelineIsRunning__whenAStageIsPostedWithAnUnknownStageType__thenRequestIsNotAcceptable() throws Exception {
        this.handler.apply(PipelineStagesPostRequest.builder()
                .pipelineId(this.runningPipelineId)
                .stageType("nosuchstagetype")
                .payload(creation -> creation.name("a-stage"))
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));
    }

    @Test
    public void givenPipelineDoesntExists__whenStageIsPosted__thenRequestIsNotAcceptable() throws Exception {
        Status400 response = this.handler.apply(PipelineStagesPostRequest.builder()
                .pipelineId("no-such-pipeline")
                .stageType("main")
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
                .stageType("main")
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
                .stage(stage -> stage
                        .name("a-stage")
                        .status(status -> status.run(StageStatus.Run.RUNNING))
                        .stageType(Stage.StageType.MAIN)
                )
                .build());

        Status400 response = this.handler.apply(PipelineStagesPostRequest.builder()
                .pipelineId(this.runningPipelineId)
                .stageType("main")
                .payload(creation -> creation.name("a-stage"))
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));

        assertThat(response.payload().code(), is(Error.Code.ILLEGAL_RESOURCE_CREATION));
        assertThat(response.payload().description(), is("state already exists"));
    }

    @Test
    public void givenAnotherStageIsRunningOfTheSame__whenStageIsPosted__thenRequestIsNotAcceptable() throws Exception {
        this.repository().stageRepository().create(PipelineStage.builder()
                .pipelineId(this.runningPipelineId)
                .stage(stage -> stage
                        .name("another-stage")
                        .stageType(Stage.StageType.MAIN)
                        .status(status -> status.run(StageStatus.Run.RUNNING)))
                .build());

        Status400 response = this.handler.apply(PipelineStagesPostRequest.builder()
                .pipelineId(this.runningPipelineId)
                .stageType("main")
                .payload(creation -> creation.name("a-stage"))
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));

        assertThat(response.payload().code(), is(Error.Code.ILLEGAL_RESOURCE_CREATION));
        assertThat(response.payload().description(), is("there's already a running stage"));
    }

    @Test
    public void givenAnotherStageOfADifferentTypeIsRunning__whenStageIsPosted__thenRequestIsNotAcceptable() throws Exception {
        this.repository().stageRepository().create(PipelineStage.builder()
                .pipelineId(this.runningPipelineId)
                .stage(stage -> stage
                        .name("another-stage")
                        .stageType(Stage.StageType.ERROR)
                        .status(status -> status.run(StageStatus.Run.RUNNING)))
                .build());

        Status400 response = this.handler.apply(PipelineStagesPostRequest.builder()
                .pipelineId(this.runningPipelineId)
                .stageType("main")
                .payload(creation -> creation.name("a-stage"))
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));

        assertThat(response.payload().code(), is(Error.Code.ILLEGAL_RESOURCE_CREATION));
        assertThat(response.payload().description(), is("there's already a running stage"));
    }
}