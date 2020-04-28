package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineStageLogsGetRequest;
import org.codingmatters.poom.ci.pipeline.api.ValueList;
import org.codingmatters.poom.ci.pipeline.api.pipelinestagelogsgetresponse.Status200;
import org.codingmatters.poom.ci.pipeline.api.pipelinestagelogsgetresponse.Status206;
import org.codingmatters.poom.ci.pipeline.api.service.repository.LogStore;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.types.LogLine;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.services.tests.Eventually;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StageLogsBrowsingTest extends AbstractPoomCITest {

    private StageLogsBrowsing handler;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        handler = new StageLogsBrowsing(this.repository());
        LogStore.Segment segment = this.repository().logStore().segment("a-pipeline", Stage.StageType.MAIN, "a-stage");
        PoomCIRepository.StageLogKey key = new PoomCIRepository.StageLogKey("a-pipeline", Stage.StageType.MAIN, "a-stage");
        for (long i = 0; i < 500; i++) {
            segment.append("content of log line " + i);
        }
        Eventually.defaults().assertThat(() -> segment.all(0L, 0L).total(), is(500L));
    }

    @Test
    public void whenNoStageType__thenInvalidQuery() {
        this.handler.apply(PipelineStageLogsGetRequest.builder()
                .pipelineId("a-pipeline").stageName("a-stage")
                .range("10-5")
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));
    }

    @Test
    public void whenInvalidStageType__thenInvalidQuery() {
        this.handler.apply(PipelineStageLogsGetRequest.builder()
                .pipelineId("a-pipeline").stageName("a-stage")
                .range("10-5")
                .stageType("no-a-stage-type")
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));
    }

    @Test
    public void invalid() throws Exception {
        this.handler.apply(PipelineStageLogsGetRequest.builder()
                .pipelineId("a-pipeline").stageName("a-stage").stageType("main")
                .range("10-5")
                .build())
                .opt().status416()
                .orElseThrow(() -> new AssertionError("should have a 416"));
    }

    @Test
    public void empty() throws Exception {
        ValueList<LogLine> list = this.handler.apply(PipelineStageLogsGetRequest.builder()
                .pipelineId("another-pipeline").stageName("another-stage").stageType("main")
                .build())
                .opt().status200().payload()
                .orElseThrow(() -> new AssertionError("should have a 200"));

        assertThat(list.toArray(), is(emptyArray()));
    }

    @Test
    public void partial() throws Exception {
        Status206 response = this.handler.apply(PipelineStageLogsGetRequest.builder()
                .pipelineId("a-pipeline").stageName("a-stage").stageType("main")
                .range("400-449")
                .build())
                .opt().status206()
                .orElseThrow(() -> new AssertionError("should have a 206"));

        assertThat(response.contentRange(), is("LogLine 400-449/500"));
        assertThat(response.xPipelineId(), is("a-pipeline"));
        assertThat(response.xStageName(), is("a-stage"));
        assertThat(response.payload().size(), is(50));

    }

    @Test
    public void complete() throws Exception {
        Status200 response = this.handler.apply(PipelineStageLogsGetRequest.builder()
                .pipelineId("a-pipeline").stageName("a-stage").stageType("main")
                .range("450-550")
                .build())
                .opt().status200()
                .orElseThrow(() -> new AssertionError("should have a 200"));

        assertThat(response.contentRange(), is("LogLine 450-499/500"));
        assertThat(response.xPipelineId(), is("a-pipeline"));
        assertThat(response.xStageName(), is("a-stage"));
        assertThat(response.payload().size(), is(50));
    }
}