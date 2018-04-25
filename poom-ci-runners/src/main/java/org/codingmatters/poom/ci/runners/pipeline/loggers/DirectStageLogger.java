package org.codingmatters.poom.ci.runners.pipeline.loggers;

import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.descriptors.StageHolder;
import org.codingmatters.poom.ci.runners.pipeline.PipelineExecutor;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.io.IOException;

public class DirectStageLogger implements PipelineExecutor.StageLogListener {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(DirectStageLogger.class);

    private final String pipilineId;
    private final StageHolder stage;
    private final PoomCIPipelineAPIClient pipelineAPIClient;

    public DirectStageLogger(String pipilineId, StageHolder stage, PoomCIPipelineAPIClient pipelineAPIClient) {
        this.pipilineId = pipilineId;
        this.stage = stage;
        this.pipelineAPIClient = pipelineAPIClient;
    }

    @Override
    public void logLine(String logLine) {
        try {
            this.pipelineAPIClient.pipelines().pipeline().pipelineStages().pipelineStage().pipelineStageLogs().patch(request -> request
                    .pipelineId(this.pipilineId)
                    .stageType(this.stage.type().name())
                    .stageName(this.stage.stage().name())
                    .payload(payloadLine -> payloadLine.content(logLine))
            );
        } catch (IOException e) {
            log.error(String.format(
                    "failed pushing pipeline %s stage %s logs",
                    this.pipilineId, this.stage),
                    e);
            log.personalData().tokenized().info("missed log for pipeline {} stage {} : {}",
                    this.pipilineId, this.stage, logLine);
        }
    }
}
