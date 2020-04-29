package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineStageLogsPatchRequest;
import org.codingmatters.poom.ci.pipeline.api.ValueList;
import org.codingmatters.poom.ci.pipeline.api.pipelinestagelogspatchresponse.Status201;
import org.codingmatters.poom.ci.pipeline.api.service.repository.LogStore;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.types.AppendedLogLine;
import org.codingmatters.poom.ci.pipeline.api.types.LogLine;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.ci.pipeline.api.types.StageStatus;
import org.codingmatters.poom.services.tests.Eventually;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class StageLogsAppendTest extends AbstractPoomCITest {

    private StageLogsAppend handler;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        handler = new StageLogsAppend(this.repository());
        this.repository().stageRepository().create(PipelineStage.builder()
                .pipelineId("a-pipeline")
                .stage(stage -> stage
                        .name("a-running-stage")
                        .stageType(Stage.StageType.MAIN)
                        .status(status -> status.run(StageStatus.Run.RUNNING))
                )
                .build());
        this.repository().stageRepository().create(PipelineStage.builder()
                .pipelineId("a-pipeline")
                .stage(stage -> stage
                        .name("a-done-stage")
                        .stageType(Stage.StageType.MAIN)
                        .status(status -> status.run(StageStatus.Run.DONE))
                )
                .build());

        LogStore.Segment segment = this.repository().logStore().segment("a-pipeline", Stage.StageType.MAIN, "a-running-stage");
        for (long i = 0; i < 500; i++) {
            segment.append("content of log line " + i);
        }
        eventually.assertThat(() -> segment.all(0L, 0L).total(), is(500L));
    }

    @Test
    public void whenNoStageType__thenRequestIsInvalid() {
        this.handler.apply(PipelineStageLogsPatchRequest.builder()
                .pipelineId("a-pipeline")
                .stageName("a-running-stage")
                .payload(AppendedLogLine.builder().content("added").build())
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));
    }

    @Test
    public void whenInvalidStageType__thenRequestIsInvalid() {
        this.handler.apply(PipelineStageLogsPatchRequest.builder()
                .pipelineId("a-pipeline")
                .stageName("a-running-stage")
                .stageType("no-quite-a-stage-type")
                .payload(AppendedLogLine.builder().content("added").build())
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));
    }

    @Test
    public void givenStageExists__whenAppendingOneLog__thenLineIsStoredAndAssignedLastIndex() throws Exception {
        LogStore.Segment segment = this.repository().logStore().segment("a-pipeline", Stage.StageType.MAIN, "a-running-stage");

        long logCount = segment.all(0, 0).total();
        Status201 response = this.handler.apply(PipelineStageLogsPatchRequest.builder()
                .pipelineId("a-pipeline")
                .stageName("a-running-stage")
                .stageType("main")
                .payload(AppendedLogLine.builder().content("added").build())
                .build())
                .opt().status201()
                .orElseThrow(() -> new AssertionError("should have a 201"));

        eventually.assertThat(() -> segment.all(500, 500).size(), is(1));
        StageLog lastLog = segment.all(500, 500).valueList().get(0);

        assertThat(response.location(), is("%API_PATH%/pipelines/a-pipeline/stages/a-running-stage/logs"));
        assertThat(segment.all(0, 0).total(), is(logCount + 1));
        assertThat(lastLog.pipelineId(), is("a-pipeline"));
        assertThat(lastLog.stageName(), is("a-running-stage"));
        assertThat(lastLog.stageType(), is(Stage.StageType.MAIN));
        assertThat(lastLog.log(), is(LogLine.builder().line(501L).content("added").build()));
    }

    @Test
    public void givenStageExists__whenAppendingManyLogs__thenLinesAreStoredAndAssignedLastIndex() throws Exception {
        LogStore.Segment segment = this.repository().logStore().segment("a-pipeline", Stage.StageType.MAIN, "a-running-stage");

        long logCount = segment.all(0, 0).total();
        ValueList.Builder<AppendedLogLine> listBuilder = new ValueList.Builder<AppendedLogLine>();
        for (int i = 1; i <= 10; i++) {
            listBuilder.with(AppendedLogLine.builder().content("added " + i).build());
        }

        Status201 response = this.handler.apply(PipelineStageLogsPatchRequest.builder()
                .pipelineId("a-pipeline")
                .stageName("a-running-stage")
                .stageType("main")
                .payload(listBuilder.build())
                .build())
                .opt().status201()
                .orElseThrow(() -> new AssertionError("should have a 201"));


        assertThat(response.location(), is("%API_PATH%/pipelines/a-pipeline/stages/a-running-stage/logs"));
        eventually.assertThat(() -> segment.all(0L, 0L).total(), is(logCount + 10L));

        List<StageLog> lastLogs = segment.all(500, 600).valueList();
        assertThat(lastLogs.size(), is(10));

        for (long i = 1; i <= 10; i++) {
            assertThat("added log " + i, lastLogs.get((int) i - 1).log(), is(LogLine.builder().line(500 + i).content("added " + i).build()));
        }
    }

    @Test
    public void givenStageIsDone__whenAddingLogs__then201_andLogIsAppended() {
        this.handler.apply(PipelineStageLogsPatchRequest.builder()
                .pipelineId("a-pipeline")
                .stageName("a-done-stage")
                .stageType("main")
                .payload(AppendedLogLine.builder().content("added").build())
                .build())
                .opt().status201()
                .orElseThrow(() -> new AssertionError("should have a 201"));

        LogStore.Segment segment = this.repository().logStore().segment("a-pipeline", Stage.StageType.MAIN, "a-done-stage");
        eventually.assertThat(() -> segment.all(0, 0).total(), is(1L));

    }

    @Test
    public void givenStageDoesntExist__whenAddingSomeLogs__thenResourceNotFound() {
        this.handler.apply(PipelineStageLogsPatchRequest.builder()
                .pipelineId("a-pipeline")
                .stageName("no-such-stage")
                .stageType("main")
                .payload(AppendedLogLine.builder().content("added").build())
                .build())
                .opt().status404()
                .orElseThrow(() -> new AssertionError("should have a 404"));
    }
}