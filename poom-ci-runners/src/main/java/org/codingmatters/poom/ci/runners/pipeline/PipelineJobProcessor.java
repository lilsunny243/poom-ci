package org.codingmatters.poom.ci.runners.pipeline;

import org.codingmatters.poom.ci.pipeline.api.PipelineGetResponse;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.pipeline.api.types.StageTermination;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.runners.pipeline.loggers.DirectStageLogger;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class PipelineJobProcessor implements JobProcessor {
    static private CategorizedLogger log = CategorizedLogger.getLogger(PipelineJobProcessor.class);

    private final Job job;
    private final PipelineContextProvider pipelineContextProvider;
    private final PipelineExecutorProvider pipelineExecutorProvider;
    private final PoomCIPipelineAPIClient pipelineAPIClient;

    public PipelineJobProcessor(Job job, PipelineContextProvider pipelineContextProvider, PipelineExecutorProvider pipelineExecutorProvider, PoomCIPipelineAPIClient pipelineAPIClient) {
        this.job = job;
        this.pipelineContextProvider = pipelineContextProvider;
        this.pipelineExecutorProvider = pipelineExecutorProvider;
        this.pipelineAPIClient = pipelineAPIClient;
    }

    @Override
    public Job process() throws JobProcessingException {
        PipelineContext context = this.initializeContext();
        PipelineExecutor executor = this.pipelineExecutorProvider.forContext(context);

        this.initializeExecution(context, executor);
        this.executeStages(context, executor);


        log.audit().info("successfully executed pipeline {}", context.pipelineId());
        return this.job
                .withStatus(Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build())
                .withProcessing(this.job.processing().withFinished(LocalDateTime.now(ZoneOffset.UTC.normalized())));
    }

    private PipelineContext initializeContext() throws JobProcessingException {
        String pipelineId = this.job.arguments().get(0);
        log.audit().info("starting pipeline {} execution", pipelineId);

        PipelineTrigger trigger = this.retrievePipelineTrigger(pipelineId);
        return this.createContext(pipelineId, trigger);
    }

    private PipelineTrigger retrievePipelineTrigger(String pipelineId) throws JobProcessingException {
        log.audit().info("retrieving pipeline {} trigger", pipelineId);
        try {
            PipelineGetResponse response = this.pipelineAPIClient.pipelines().pipeline().get(req -> req.pipelineId(pipelineId));
            return response.opt().status200()
                    .payload().trigger()
                    .orElseThrow(() -> {
                        String errorToken = log.tokenized().error("while retrieving trigger for pipeline {}, got response : {}",
                                pipelineId, response);
                        return new JobProcessingException("couldn't retrieve pipeline trigger, see logs with " + errorToken);
                    });
        } catch (IOException e) {
            throw new JobProcessingException("failed accessing pipeline API", e);
        }
    }

    private PipelineContext createContext(String pipelineId, PipelineTrigger trigger) throws JobProcessingException {
        try {
            return this.pipelineContextProvider.pipelineContext(pipelineId, trigger);
        } catch (IOException e) {
            String errorToken = log.personalData().tokenized().error("couldn't initialize pipeline context", e);
            throw new JobProcessingException("error initializing pipeline context, see logs with error-token=" + errorToken);
        }
    }

    private void initializeExecution(PipelineContext context, PipelineExecutor executor) throws JobProcessingException {
        try {
            executor.initialize();
            log.audit().info("pipeline {} executor initialized", context.pipelineId());
        } catch (IOException e) {
            String errorToken = log.personalData().tokenized().error(String.format(
                    "couldn't initialize pipeline %s execution",
                    context.pipelineId()),
                    e);
            throw new JobProcessingException("error initializing pipeline execution, see logs with error-token=" + errorToken);
        }
    }

    private void executeStages(PipelineContext context, PipelineExecutor executor) throws JobProcessingException {
        for (String stage : context.stages()) {
            this.executeStage(context, executor, stage);
        }
    }

    private void executeStage(PipelineContext context, PipelineExecutor executor, String stage) throws JobProcessingException {
        log.audit().info("executing pipeline {} stage {}", context.pipelineId(), stage);
        try {
            this.notifyStageExecutionStart(context, stage);
            StageTermination.Exit status = executor.execute(stage, this.stageLogListener(context, stage));
            this.notifyStageExecutionEnd(context, stage, status);
        } catch (IOException e) {
            String errorToken = log.tokenized().error(String.format(
                    "error executing pipeline %s stage %s",
                    context.pipelineId(), stage),
                    e);
            throw new JobProcessingException("error executing pipeline stage, see logs with error-token=" + errorToken);
        }
    }

    private void notifyStageExecutionStart(PipelineContext context, String stage) throws JobProcessingException {
        try {
            this.pipelineAPIClient.pipelines().pipeline().pipelineStages().post(req -> req
                    .pipelineId(context.pipelineId())
                    .payload(creation -> creation.name(stage))
            );
        } catch (IOException e) {
            String errorToken = log.tokenized().error(String.format(
                    "error notifying pipeline %s stage %s execution start",
                    context.pipelineId(), stage),
                    e);
            throw new JobProcessingException("error executing pipeline stage, see logs with error-token=" + errorToken);
        }
    }

    private void notifyStageExecutionEnd(PipelineContext context, String stage, StageTermination.Exit status) throws JobProcessingException {
        try {
            this.pipelineAPIClient.pipelines().pipeline().pipelineStages().pipelineStage().patch(req -> req
                    .pipelineId(context.pipelineId()).stageName(stage)
                    .payload(term -> term.exit(status))
            );
        } catch (IOException e) {
            String errorToken = log.tokenized().error(String.format(
                    "error notifying pipeline %s stage %s execution start",
                    context.pipelineId(), stage),
                    e);
            throw new JobProcessingException("error executing pipeline stage, see logs with error-token=" + errorToken);
        }
    }

    private PipelineExecutor.StageLogListener stageLogListener(PipelineContext context, String stage) {
        return new DirectStageLogger(context.pipelineId(), stage, this.pipelineAPIClient);
    }
}
