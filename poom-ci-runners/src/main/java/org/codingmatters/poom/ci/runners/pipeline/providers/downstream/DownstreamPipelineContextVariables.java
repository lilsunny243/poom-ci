package org.codingmatters.poom.ci.runners.pipeline.providers.downstream;

import org.codingmatters.poom.ci.pipeline.api.UpstreamBuildTriggerGetResponse;
import org.codingmatters.poom.ci.pipeline.api.UpstreamBuildTriggerPatchResponse;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.runners.pipeline.PipelineVariables;
import org.codingmatters.poom.ci.runners.pipeline.providers.gh.AbstractGitHubPipelineContextProvider;
import org.codingmatters.poom.ci.triggers.UpstreamBuild;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.io.IOException;

public class DownstreamPipelineContextVariables {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(DownstreamPipelineContextVariables.class);

    private final String pipelineId;
    private final PipelineTrigger trigger;
    private final PoomCIPipelineAPIClient pipelineAPIClient;

    public DownstreamPipelineContextVariables(String pipelineId, PipelineTrigger trigger, PoomCIPipelineAPIClient pipelineAPIClient) {
        this.pipelineId = pipelineId;
        this.trigger = trigger;
        this.pipelineAPIClient = pipelineAPIClient;
    }

    public PipelineVariables variables() throws AbstractGitHubPipelineContextProvider.ProcessingException {
        UpstreamBuild build = this.retrieveTrigger(this.trigger);
        this.consumeTrigger(build);

        //git|git@github.com:flexiooss/codingmatters-rest.git|master
        String[] splittedCheckoutSpec = build.downstream().checkoutSpec().split("\\|");

        return PipelineVariables.builder()
                .pipelineId(this.pipelineId)
                .repositoryId(build.downstream().id())
                .repository(build.downstream().name())
                .repositoryUrl(splittedCheckoutSpec[1])
                .branch(splittedCheckoutSpec[2])
                .changeset(null)
                .checkoutSpec(build.downstream().checkoutSpec())
                .build();
    }

    private void consumeTrigger(UpstreamBuild build) throws AbstractGitHubPipelineContextProvider.ProcessingException {
        try {
            UpstreamBuildTriggerPatchResponse response = this.pipelineAPIClient.triggers().upstreamBuildTriggers().upstreamBuildTrigger().patch(
                    req -> req.triggerId(this.trigger.triggerId()).payload(build.withConsumed(true))
            );
        } catch (IOException e) {
            throw new AbstractGitHubPipelineContextProvider.ProcessingException("failed updating build as being consumed", e);
        }
    }

    private UpstreamBuild retrieveTrigger(PipelineTrigger trigger) throws AbstractGitHubPipelineContextProvider.ProcessingException {
        try {
            UpstreamBuildTriggerGetResponse response = this.pipelineAPIClient.triggers().upstreamBuildTriggers().upstreamBuildTrigger().get(
                    req -> req.triggerId(trigger.triggerId())
            );

            return response.opt().status200().payload()
                    .orElseThrow(() -> {
                        String token = log.tokenized().error("while retrieving upstream build trigger, received unexpected response : {}", response);
                        return new AbstractGitHubPipelineContextProvider.ProcessingException("error getting pipeline trigger, see logs with token " + token);
                    });
        } catch (IOException e) {
            throw new AbstractGitHubPipelineContextProvider.ProcessingException("failed accessing pipeline api", e);
        }
    }
}
