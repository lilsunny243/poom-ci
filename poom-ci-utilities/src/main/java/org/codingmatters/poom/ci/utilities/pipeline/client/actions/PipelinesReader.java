package org.codingmatters.poom.ci.utilities.pipeline.client.actions;

import org.codingmatters.poom.ci.pipeline.api.PipelinesGetRequest;
import org.codingmatters.poom.ci.pipeline.api.optional.OptionalPipelinesGetResponse;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class PipelinesReader extends BasePipelineReader{

    public PipelinesReader(String baseUrl) {
        super(baseUrl);
    }

    public void readPipelines(BiConsumer<Pipeline,Optional<GithubPushEvent>> doWithPipeline) throws IOException {
        int step = 50;
        int start = 0;
        int end = start + step - 1;

        OptionalPipelinesGetResponse response;
        do {
            response = this.client().pipelines().get(PipelinesGetRequest.builder()
                    .range(String.format("%s-%s", start, end))
                    .build()).opt();

            if(! this.isValid(response)) {
                throw new IOException("invalid response : " + response.get());
            } else {
                for (Pipeline pipeline : this.pipelines(response)) {
                    GithubPushEvent trigger = null;
                    if(pipeline.trigger().type().equals(PipelineTrigger.Type.GITHUB_PUSH)) {
                        trigger = this.client().triggers().githubTriggers().githubTrigger().get(req -> req.triggerId(pipeline.trigger().triggerId()))
                                .opt().status200().payload().get();
                    }

                    doWithPipeline.accept(pipeline, Optional.ofNullable(trigger));
                }
            }

            start = end + 1;
            end = start + step - 1;
        } while (! response.status200().isPresent());
    }

    private List<Pipeline> pipelines(OptionalPipelinesGetResponse response) {
        List<Pipeline> result = new LinkedList<>();

        response.status206().ifPresent(status -> status.payload().forEach(pipeline -> result.add(pipeline)));
        response.status200().ifPresent(status -> status.payload().forEach(pipeline -> result.add(pipeline)));

        return result;
    }

    private boolean isValid(OptionalPipelinesGetResponse response) {
        return response.status200().isPresent() || response.status206().isPresent();
    }
}
