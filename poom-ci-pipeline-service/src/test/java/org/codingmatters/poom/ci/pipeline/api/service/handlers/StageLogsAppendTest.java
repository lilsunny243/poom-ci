package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineStageLogsPatchRequest;
import org.codingmatters.poom.ci.pipeline.api.ValueList;
import org.codingmatters.poom.ci.pipeline.api.pipelinestagelogspatchresponse.Status201;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.types.AppendedLogLine;
import org.codingmatters.poom.ci.pipeline.api.types.LogLine;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.ci.pipeline.api.types.StageStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StageLogsAppendTest extends AbstractPoomCITest {

    private StageLogsAppend handler = new StageLogsAppend(this.repository());


    @Before
    public void setUp() throws Exception {
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

        for (long i = 0; i < 500; i++) {
            this.repository().logRepository().create(StageLog.builder()
                    .pipelineId("a-pipeline")
                    .stageName("a-running-stage")
                    .stageType(Stage.StageType.MAIN)
                    .log(LogLine.builder()
                            .line(i).content("content of log line " + i)
                            .build())
                    .build());
        }
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
        long logCount = this.repository().logRepository().all(0, 0).total();
        Status201 response = this.handler.apply(PipelineStageLogsPatchRequest.builder()
                .pipelineId("a-pipeline")
                .stageName("a-running-stage")
                .stageType("main")
                .payload(AppendedLogLine.builder().content("added").build())
                .build())
                .opt().status201()
                .orElseThrow(() -> new AssertionError("should have a 201"));
        StageLog lastLog = this.repository().logRepository().all(500, 500).valueList().get(0);

        assertThat(response.location(), is("%API_PATH%/pipelines/a-pipeline/stages/a-running-stage/logs"));
        assertThat(this.repository().logRepository().all(0, 0).total(), is(logCount + 1));
        assertThat(lastLog.pipelineId(), is("a-pipeline"));
        assertThat(lastLog.stageName(), is("a-running-stage"));
        assertThat(lastLog.stageType(), is(Stage.StageType.MAIN));
        assertThat(lastLog.log(), is(LogLine.builder().line(501L).content("added").build()));
    }

    @Test
    public void givenStageExists__whenAppendingManyLogs__thenLinesAreStoredAndAssignedLastIndex() throws Exception {
        long logCount = this.repository().logRepository().all(0, 0).total();
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

        List<StageLog> lastLogs = this.repository().logRepository().all(500, 600).valueList();

        assertThat(response.location(), is("%API_PATH%/pipelines/a-pipeline/stages/a-running-stage/logs"));
        assertThat(this.repository().logRepository().all(0, 0).total(), is(logCount + 10));

        assertThat(lastLogs.size(), is(10));

        for (long i = 1; i <= 10; i++) {
            assertThat("added log " + i, lastLogs.get((int) i - 1).log(), is(LogLine.builder().line(500 + i).content("added " + i).build()));
        }
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

    @Test
    public void givenStageIsDone__whenAddingLogs__thenIllegalCollectionChange() {
        this.handler.apply(PipelineStageLogsPatchRequest.builder()
                .pipelineId("a-pipeline")
                .stageName("a-done-stage")
                .stageType("main")
                .payload(AppendedLogLine.builder().content("added").build())
                .build())
                .opt().status400()
                .orElseThrow(() -> new AssertionError("should have a 400"));
    }
}