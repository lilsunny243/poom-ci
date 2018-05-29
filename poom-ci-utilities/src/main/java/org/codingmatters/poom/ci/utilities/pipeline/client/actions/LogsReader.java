package org.codingmatters.poom.ci.utilities.pipeline.client.actions;

import org.codingmatters.poom.ci.pipeline.api.PipelineStageLogsGetRequest;
import org.codingmatters.poom.ci.pipeline.api.optional.OptionalPipelineStageLogsGetResponse;
import org.codingmatters.poom.ci.pipeline.api.types.LogLine;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class LogsReader extends BasePipelineReader {


    private final String pipeline;
    private final String stageType;
    private final String stage;

    public LogsReader(String baseUrl, String pipeline, String stageType, String stage) {
        super(baseUrl);
        this.pipeline = pipeline;
        this.stageType = stageType;
        this.stage = stage;
    }

    public void readLines(Consumer<LogLine> doWithLine) throws IOException {
        int step = 50;
        int start = 0;
        int end = start + step - 1;
        OptionalPipelineStageLogsGetResponse response = null;
        do {
            response = this.client().pipelines().pipeline().pipelineStages()
                    .pipelineStage().pipelineStageLogs().get(PipelineStageLogsGetRequest.builder()
                            .pipelineId(pipeline)
                            .stageType(stageType)
                            .stageName(stage)
                            .range(String.format("%s-%s", start, end))
                            .build()
                    ).opt();

            if (!this.valid(response)) {
                throw new IOException("got unexpected response from pipeline service : " + response.get());
            }

            for (LogLine logLine : this.readNextLines(response)) {
                doWithLine.accept(logLine);
            }

            start = end + 1;
            end = start + step - 1;
        } while (! response.status200().isPresent());
    }

    private boolean valid(OptionalPipelineStageLogsGetResponse response) {
        return response.status200().isPresent() || response.status206().isPresent();
    }

    private List<LogLine> readNextLines(OptionalPipelineStageLogsGetResponse response) {
        List<LogLine> result = new LinkedList<>();

        response.status200().ifPresent(status -> status.payload().forEach(logLine -> result.add(logLine)));
        response.status206().ifPresent(status -> status.payload().forEach(logLine -> result.add(logLine)));

        return result;
    }

}
