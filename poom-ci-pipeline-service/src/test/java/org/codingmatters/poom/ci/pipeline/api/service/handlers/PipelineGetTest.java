package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineGetRequest;
import org.codingmatters.poom.ci.pipeline.api.pipelinegetresponse.Status200;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PipelineGetTest extends AbstractPoomCITest {

    private PipelineGet handler = new PipelineGet(this.repository());

    @Test
    public void whenTriggerNotFound__then404() {
        this.handler.apply(PipelineGetRequest.builder().pipelineId("not-found").build()).opt()
                .status404()
                .orElseThrow(() -> new AssertionError("should have a 404"));
    }

    @Test
    public void whenEntityFound__then200WithTriggerPayload() throws Exception {
        Entity<Pipeline> pipeline = this.repository().pipelineRepository().create(Pipeline.builder().status(status -> status.run(Status.Run.RUNNING)).build());

        Status200 found = this.handler.apply(PipelineGetRequest.builder().pipelineId(pipeline.id()).build()).opt()
                .status200()
                .orElseThrow(() -> new AssertionError("should have a 200"));

        assertThat(found.payload(), is(pipeline.value()));
        assertThat(found.xEntityId(), is(pipeline.id()));
    }
}