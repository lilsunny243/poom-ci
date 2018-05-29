package org.codingmatters.poom.ci.utilities.pipeline.client.actions;

import org.codingmatters.poom.ci.pipeline.api.PipelineStagesGetRequest;
import org.codingmatters.poom.ci.pipeline.api.optional.OptionalPipelineStagesGetResponse;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class StagesReader extends BasePipelineReader {
    private final String pipelineId;
    private final String stageType;

    public StagesReader(String baseUrl, String pipelineId, String stageType) {
        super(baseUrl);
        this.pipelineId = pipelineId;
        this.stageType = stageType;
    }

    public void readStages(Consumer<Stage> doWithStage) throws IOException {
        OptionalPipelineStagesGetResponse response;
        int step = 50;
        int start = 0;
        int end = start + step - 1;

        do {
            response = this.client().pipelines().pipeline().pipelineStages().get(PipelineStagesGetRequest.builder()
                    .pipelineId(this.pipelineId)
                    .stageType(this.stageType)
                    .range(String.format("", start, end))
                    .build()
            ).opt();

            for (Stage stage : this.stages(response)) {
                doWithStage.accept(stage);
            }

            if(! this.isValid(response)) {
                throw new IOException("invalid response " + response);
            }

        } while(! response.status200().isPresent());
    }

    private List<Stage> stages(OptionalPipelineStagesGetResponse response) {
        List<Stage> result = new LinkedList<>();

        response.status206().ifPresent(status -> status.payload().forEach(stage -> result.add(stage)));
        response.status200().ifPresent(status -> status.payload().forEach(stage -> result.add(stage)));

        return result;
    }

    private boolean isValid(OptionalPipelineStagesGetResponse response) {
        return response.status200().isPresent() || response.status206().isPresent();
    }


}
