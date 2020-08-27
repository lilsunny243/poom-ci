package org.codingmatters.poom.ci.runners.pipeline;

import org.codingmatters.poom.ci.pipeline.api.*;
import org.codingmatters.poom.ci.pipeline.api.types.*;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIHandlersClient;
import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.pipeline.descriptors.StageHolder;
import org.codingmatters.poom.services.tests.Eventually;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class PipelineJobProcessorTest {

    private PoomCIPipelineAPIClient pipelineClient = new PoomCIPipelineAPIHandlersClient(
            new PoomCIPipelineAPIHandlers.Builder()
                    .pipelineGetHandler(this::pipelineGet)
                    .pipelineStagesPostHandler(this::stagePost)
                    .pipelineStagePatchHandler(this::stagePatch)
                    .pipelineStageLogsPatchHandler(this::logsPatch)
                    .pipelinePatchHandler(this::pipelinePatch)
                    .build(),
            Executors.newFixedThreadPool(4)
    );

    private final List<PipelineGetRequest> pipelineGetCalls = Collections.synchronizedList(new LinkedList<>());
    private PipelineGetResponse pipelineGet(PipelineGetRequest request) {
        pipelineGetCalls.add(request);
        return PipelineGetResponse.builder()
                .status200(status -> status
                        .xEntityId(request.pipelineId())
                        .payload(pipeline -> pipeline
                                .trigger(trigger -> trigger.triggerId("trigger-id").type(PipelineTrigger.Type.GITHUB_PUSH))
                        )
                )
                .build();
    }

    private final List<PipelinePatchRequest> pipelinePatchCalls = Collections.synchronizedList(new LinkedList<>());
    private PipelinePatchResponse pipelinePatch(PipelinePatchRequest request) {
        pipelinePatchCalls.add(request);
        return PipelinePatchResponse.builder()
                .status200(status -> status.payload(pipe -> pipe.status(st -> st.run(org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status.Run.DONE))))
                .build();
    }

    private final List<PipelineStagesPostRequest> stagePostCalls = Collections.synchronizedList(new LinkedList<>());
    private PipelineStagesPostResponse stagePost(PipelineStagesPostRequest request) {
        stagePostCalls.add(request);
        return PipelineStagesPostResponse.builder()
                .status201(status -> status.xEntityId(request.payload().name()))
                .build();
    }

    private final List<PipelineStagePatchRequest> stagePatchCalls = Collections.synchronizedList(new LinkedList<>());
    private PipelineStagePatchResponse stagePatch(PipelineStagePatchRequest request) {
        stagePatchCalls.add(request);
        return PipelineStagePatchResponse.builder()
                .status200(status -> status.payload(stage -> stage
                        .name(request.stageName())
                        .status(st -> st.run(StageStatus.Run.DONE).exit(StageStatus.Exit.valueOf(request.payload().exit().name())))))
                .build();
    }

    private final List<PipelineStageLogsPatchRequest> logsPatchCalls = Collections.synchronizedList(new LinkedList<>());
    private PipelineStageLogsPatchResponse logsPatch(PipelineStageLogsPatchRequest request) {
        logsPatchCalls.add(request);
        return PipelineStageLogsPatchResponse.builder()
                .status201(status -> status.location(""))
                .build();
    }

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    private final List<PipelineTrigger> contextProviderCalls = Collections.synchronizedList(new LinkedList<>());
    private PipelineContext.PipelineContextProvider testContextProvider = (pipelineId, trigger) -> {
        contextProviderCalls.add(trigger);

        PipelineVariables vars = PipelineVariables.builder()
                .pipelineId(pipelineId)
                .repository("repo")
                .branch("master")
                .changeset("1234567890")
                .build();

        return new PipelineContext(vars, Pipeline.builder()
                .stages(
                        stage -> stage.name("stage1"),
                        stage -> stage.name("stage2")
                )
                .build(), dir.newFolder(), dir.newFolder());
    };

    private final AtomicInteger execInitCount = new AtomicInteger(0);
    private final List<String> executedStages = Collections.synchronizedList(new LinkedList<>());
    private PipelineExecutor testExecutor(PipelineContext context) {
        return new PipelineExecutor() {
            @Override
            public void initialize() throws IOException {
                execInitCount.incrementAndGet();
            }

            @Override
            public boolean isExecutable(StageHolder stage) throws InvalidStageRestrictionException {
                return true;
            }

            @Override
            public StageTermination.Exit execute(StageHolder stage, StageLogListener logListener) throws IOException {
                executedStages.add(stage.stage().name());
                for (int i = 0; i < 3; i++) {
                    System.out.println("logging  :: " + stage.stage().name() + " log " + (i+1));
                    logListener.logLine(stage.stage().name() + " log " + (i+1));
                }
                return StageTermination.Exit.SUCCESS;
            }
        };
    }

    @Test
    public void nominalLogic() throws Exception {
        Job job = Job.builder()
                .category("poom-ci").name("pipeline").arguments("pipeline-id")
                .status(status -> status.run(Status.Run.RUNNING))
                .processing(processing -> processing.submitted(LocalDateTime.now(ZoneOffset.UTC.normalized()).minus(1, ChronoUnit.MINUTES)))
                .build();

        job = new PipelineJobProcessor(job, this.testContextProvider, this::testExecutor, this.pipelineClient).process();

        assertThat(this.pipelineGetCalls, hasSize(1));
        assertThat(this.contextProviderCalls, hasSize(1));

        assertThat(this.execInitCount.get(), is(1));
        assertThat(this.executedStages, contains("stage1", "stage2"));

        assertThat(this.stagePostCalls, hasSize(2));
        assertThat(this.stagePostCalls.get(0).payload().name(), is("stage1"));
        assertThat(this.stagePostCalls.get(0).stageType(), is(Stage.StageType.MAIN.name()));
        assertThat(this.stagePostCalls.get(1).payload().name(), is("stage2"));
        assertThat(this.stagePostCalls.get(1).stageType(), is(Stage.StageType.MAIN.name()));

        assertThat(this.stagePatchCalls, hasSize(2));
        assertThat(this.stagePatchCalls.get(0).stageName(), is("stage1"));
        assertThat(this.stagePostCalls.get(0).stageType(), is(Stage.StageType.MAIN.name()));
        assertThat(this.stagePatchCalls.get(1).stageName(), is("stage2"));
        assertThat(this.stagePostCalls.get(1).stageType(), is(Stage.StageType.MAIN.name()));

        assertThat(this.pipelinePatchCalls, hasSize(2));
        assertThat(this.pipelinePatchCalls.get(0).pipelineId(), is("pipeline-id"));
        assertThat(this.pipelinePatchCalls.get(0).payload(), is(PipelineTermination.builder().run(PipelineTermination.Run.RUNNING).build()));
        assertThat(this.pipelinePatchCalls.get(1).pipelineId(), is("pipeline-id"));
        assertThat(this.pipelinePatchCalls.get(1).payload(), is(PipelineTermination.builder().exit(PipelineTermination.Exit.SUCCESS).build()));

        Eventually.timeout(30, TimeUnit.SECONDS).assertThat(() -> {
            List<String> logs = new LinkedList<>();
            this.logsPatchCalls.stream().map(request -> request.payload().stream().map(line -> line.content()).collect(Collectors.toList())).forEach(line -> logs.addAll(line));

            System.out.println(logs);
            return logs;
        }, contains("stage1 log 1", "stage1 log 2", "stage1 log 3", "stage2 log 1", "stage2 log 2", "stage2 log 3"));

        assertThat(job.status().run(), is(Status.Run.DONE));
        assertThat(job.status().exit(), is(Status.Exit.SUCCESS));
    }
}