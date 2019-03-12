package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelinePatchRequest;
import org.codingmatters.poom.ci.pipeline.api.pipelinepatchresponse.Status200;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTermination;
import org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PipelineUpdateTest extends AbstractPoomCITest {

    private PipelineUpdate handler;

    private String runningPipelineId;
    private String donePipelineId;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        handler = new PipelineUpdate(this.repository());
        Entity<Pipeline> pipe = this.repository().pipelineRepository().create(Pipeline.builder()
                .status(Status.builder()
                        .run(Status.Run.RUNNING)
                        .triggered(LocalDateTime.now(ZoneOffset.UTC.normalized()).minus(10, ChronoUnit.MINUTES))
                        .build())
                .build());
        pipe = this.repository().pipelineRepository().update(pipe, pipe.value().withId(pipe.id()));
        this.runningPipelineId = pipe.id();

        pipe = this.repository().pipelineRepository().create(Pipeline.builder()
                .status(Status.builder()
                        .run(Status.Run.DONE)
                        .exit(Status.Exit.SUCCESS)
                        .triggered(LocalDateTime.now(ZoneOffset.UTC.normalized()).minus(10, ChronoUnit.MINUTES))
                        .finished(LocalDateTime.now(ZoneOffset.UTC.normalized()).minus(5, ChronoUnit.MINUTES))
                        .build())
                .build());
        pipe = this.repository().pipelineRepository().update(pipe, pipe.value().withId(pipe.id()));
        this.donePipelineId = pipe.id();
    }

    @Test
    public void givenPipelineIsRunning__whenPatching__thenStatusUpdated() {
        Status200 response = this.handler.apply(PipelinePatchRequest.builder()
                .pipelineId(this.runningPipelineId)
                .payload(termination -> termination.exit(PipelineTermination.Exit.SUCCESS))
                .build())
                .opt().status200()
                .orElseThrow(() -> new AssertionError("should have a 200"));

        assertThat(response.payload().id(), is(this.runningPipelineId));
        assertThat(response.payload().status().run(), is(Status.Run.DONE));
        assertThat(response.payload().status().exit(), is(Status.Exit.SUCCESS));
        assertThat(response.payload().status().finished(), is(notNullValue()));
        assertTrue(response.payload().status().triggered().isBefore(response.payload().status().finished()));
    }

    @Test
    public void givenPipelineIsNotRunning__whenPatching__theIllegalResourceChange() {
        this.handler.apply(PipelinePatchRequest.builder()
                .pipelineId(this.donePipelineId)
                .payload(termination -> termination.exit(PipelineTermination.Exit.FAILURE))
                .build()).opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));
    }

    @Test
    public void givenPipelineDoesntExist__whenPatching__thenResourceNotFound() {
        this.handler.apply(PipelinePatchRequest.builder()
                .pipelineId("not-found-pipeline")
                .payload(termination -> termination.exit(PipelineTermination.Exit.FAILURE))
                .build()).opt().status404()
                .orElseThrow(() -> new AssertionError("should have a 404"));
    }

    @Test
    public void givenPipelineIsRunning__whenPatchingWithoutExitStatus__thenIllegalResourceChange() {
        this.handler.apply(PipelinePatchRequest.builder()
                .pipelineId(this.runningPipelineId)
                .payload(PipelineTermination.builder().build())
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));
    }
}