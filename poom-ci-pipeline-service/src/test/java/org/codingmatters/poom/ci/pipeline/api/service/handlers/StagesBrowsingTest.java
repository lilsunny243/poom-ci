package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineStagesGetRequest;
import org.codingmatters.poom.ci.pipeline.api.pipelinestagesgetresponse.Status200;
import org.codingmatters.poom.ci.pipeline.api.pipelinestagesgetresponse.Status206;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.ci.pipeline.api.types.StageStatus;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StagesBrowsingTest extends AbstractPoomCITest {

    private StagesBrowsing handler = new StagesBrowsing(this.repository());

    @Before
    public void setUp() throws Exception {
        for (int i = 0; i < 500; i++) {
            this.repository().stageRepository().create(PipelineStage.builder()
                    .pipelineId("pipeline-" + (i % 2))
                    .stage(Stage.builder()
                            .name("stage-" + i)
                            .status(status -> status.run(StageStatus.Run.DONE).exit(StageStatus.Exit.SUCCESS))
                            .build())
                    .build());
        }
    }

    @Test
    public void invalid() {
        this.handler.apply(PipelineStagesGetRequest.builder()
                .pipelineId("pipeline-1")
                .range("yopyop tagada")
                .build())
                .opt().status416().orElseThrow(() -> new AssertionError("should have a 416"));
    }

    @Test
    public void empty() {
        Status200 response = this.handler.apply(PipelineStagesGetRequest.builder()
                .pipelineId("no-such-pipeline")
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
                .range("0-9")
                .build())
                .opt().status206()
                .orElseThrow(() -> new AssertionError("should have a 206"));


        assertThat(response.contentRange(), is("Stage 0-9/250"));
    }

    @Test
    public void complete() {
        Status200 response = this.handler.apply(PipelineStagesGetRequest.builder()
                .pipelineId("pipeline-1")
                .range("0-300")
                .build())
                .opt().status200()
                .orElseThrow(() -> new AssertionError("should have a 200"));

        assertThat(response.contentRange(), is("Stage 0-249/250"));

    }
}