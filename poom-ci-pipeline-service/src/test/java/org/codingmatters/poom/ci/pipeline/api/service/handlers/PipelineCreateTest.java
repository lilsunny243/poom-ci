package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelinesPostRequest;
import org.codingmatters.poom.ci.pipeline.api.pipelinespostresponse.Status201;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class PipelineCreateTest extends AbstractPoomCITest {

    private final AtomicReference<Pipeline> lastCreatedPipeline = new AtomicReference<>(null);
    private PipelineCreate handler;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        handler = new PipelineCreate(this.repository(), pipeline -> lastCreatedPipeline.set(pipeline));
    }

    @Test
    public void givenTriggerIsWellFormatted__whenPosted__thenPipelineCreated() throws Exception {
        Status201 response = this.handler.apply(PipelinesPostRequest.builder()
                .payload(payload -> payload
                        .type(PipelineTrigger.Type.GITHUB_PUSH)
                        .triggerId("12")
                )
                .build())
                .opt().status201()
                .orElseThrow(() -> new AssertionError("should have a 201"));

        Entity<Pipeline> pipeline = this.repository().pipelineRepository().retrieve(response.xEntityId());

        assertThat(response.location(), is("%API_PATH%/pipelines/" + pipeline.id()));

        assertThat(pipeline.value().id(), is(pipeline.id()));

        assertThat(pipeline.value().status().run(), is(Status.Run.RUNNING));
        assertThat(pipeline.value().status().triggered(), is(notNullValue()));
        assertThat(pipeline.value().trigger(), is(PipelineTrigger.builder()
                .type(PipelineTrigger.Type.GITHUB_PUSH)
                .triggerId("12")
                .build()));

        assertThat(this.lastCreatedPipeline.get(), is(pipeline.value()));
    }

    @Test
    public void givenNoTrigger__whenPosted__thenIllegalResourceCreated() throws Exception {
        this.handler.apply(PipelinesPostRequest.builder()
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));
    }

    @Test
    public void givenTriggerMissesId__whenPosted__thenIllegalResourceCreated() throws Exception {
        this.handler.apply(PipelinesPostRequest.builder()
                .payload(payload -> payload
                        .type(PipelineTrigger.Type.GITHUB_PUSH)
                )
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));
    }

    @Test
    public void givenTriggerMissesType__whenPosted__thenIllegalResourceCreated() throws Exception {
        this.handler.apply(PipelinesPostRequest.builder()
                .payload(payload -> payload
                        .triggerId("12")
                )
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));
    }

}