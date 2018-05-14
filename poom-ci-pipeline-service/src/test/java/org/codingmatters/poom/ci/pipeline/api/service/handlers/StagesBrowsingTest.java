package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineStagesGetRequest;
import org.codingmatters.poom.ci.pipeline.api.pipelinestagesgetresponse.Status200;
import org.codingmatters.poom.ci.pipeline.api.pipelinestagesgetresponse.Status206;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StagesBrowsingTest extends AbstractPoomCITest {

    private StagesBrowsing handler = new StagesBrowsing(this.repository());

    @Before
    public void setUp() throws Exception {
        this.createSomeStages("pipeline-0");
        this.createSomeStages("pipeline-1");
    }

    @Test
    public void whenRangeIsInvalid__thenRequestIsInvalid() {
        this.handler.apply(PipelineStagesGetRequest.builder()
                .pipelineId("pipeline-1")
                .stageType("main")
                .range("yopyop tagada")
                .build())
                .opt().status416().orElseThrow(() -> new AssertionError("should have a 416"));
    }

    @Test
    public void whenNoStageType__thenRequestIsInvalid() {
        this.handler.apply(PipelineStagesGetRequest.builder()
                .pipelineId("pipeline-1")
                .range("0-9")
                .build())
                .opt().status416().orElseThrow(() -> new AssertionError("should have a 416"));
    }

    @Test
    public void whenStageTypeIsUnregistered__thenRequestIsInvalid() {
        this.handler.apply(PipelineStagesGetRequest.builder()
                .pipelineId("pipeline-1")
                .stageType("no such stage type")
                .range("0-9")
                .build())
                .opt().status416().orElseThrow(() -> new AssertionError("should have a 416"));
    }

    @Test
    public void empty() {
        Status200 response = this.handler.apply(PipelineStagesGetRequest.builder()
                .pipelineId("no-such-pipeline")
                .stageType("main")
                .range("0-9")
                .build())
                .opt().status200()
                .orElseThrow(() -> new AssertionError("should have a 200"));

        assertThat(response.contentRange(), is("Stage 0-0/0"));
    }

    @Test
    public void partial() {
        Status206 response = this.handler.apply(PipelineStagesGetRequest.builder()
                .pipelineId("pipeline-0")
                .stageType("main")
                .range("0-9")
                .build())
                .opt().status206()
                .orElseThrow(() -> new AssertionError("should have a 206"));


        assertThat(response.contentRange(), is("Stage 0-9/100"));
    }

    @Test
    public void complete() {
        Status200 response = this.handler.apply(PipelineStagesGetRequest.builder()
                .pipelineId("pipeline-1")
                .stageType("main")
                .range("0-300")
                .build())
                .opt().status200()
                .orElseThrow(() -> new AssertionError("should have a 200"));

        assertThat(response.contentRange(), is("Stage 0-99/100"));

    }
}