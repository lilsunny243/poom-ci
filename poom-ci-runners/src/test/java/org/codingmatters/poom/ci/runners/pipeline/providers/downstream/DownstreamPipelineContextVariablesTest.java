package org.codingmatters.poom.ci.runners.pipeline.providers.downstream;

import org.codingmatters.poom.ci.pipeline.api.PoomCIPipelineAPIHandlers;
import org.codingmatters.poom.ci.pipeline.api.UpstreamBuildTriggerGetRequest;
import org.codingmatters.poom.ci.pipeline.api.UpstreamBuildTriggerGetResponse;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIHandlersClient;
import org.codingmatters.poom.ci.runners.pipeline.PipelineVariables;
import org.junit.Test;

import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class DownstreamPipelineContextVariablesTest {

    private PoomCIPipelineAPIClient pipelineAPIClient = new PoomCIPipelineAPIHandlersClient(
            new PoomCIPipelineAPIHandlers.Builder()
                    .upstreamBuildTriggerGetHandler(this::trigger)
                    .build(),
            Executors.newFixedThreadPool(4));

    private UpstreamBuildTriggerGetResponse trigger(UpstreamBuildTriggerGetRequest request) {
        return UpstreamBuildTriggerGetResponse.builder()
                .status200(status -> status.payload(build -> build
                        .upstream(down -> down.id("up-repo-id").name("flexiooss/codingmatters-value-objects")
                                .checkoutSpec("git|git@github.com:flexiooss/codingmatters-value-objects.git|master"))
                        .downstream(down -> down.id("down-repo-id").name("flexiooss/codingmatters-rest")
                                .checkoutSpec("git|git@github.com:flexiooss/codingmatters-rest.git|master"))
                ))
                .build();
    }

    @Test
    public void name() throws Exception {
        PipelineVariables vars = new DownstreamPipelineContextVariables(
                "12",
                PipelineTrigger.builder()
                        .triggerId("42").type(PipelineTrigger.Type.UPSTREAM_BUILD)
                        .build(),
                this.pipelineAPIClient)
                .variables();

        assertThat(vars.pipelineId(), is("12"));
        assertThat(vars.repositoryId(), is("down-repo-id"));
        assertThat(vars.repository(), is("flexiooss/codingmatters-rest"));
        assertThat(vars.repositoryUrl(), is("git@github.com:flexiooss/codingmatters-rest.git"));
        assertThat(vars.branch(), is("master"));
        assertThat(vars.checkoutSpec(), is("git|git@github.com:flexiooss/codingmatters-rest.git|master"));
        assertThat(vars.changeset(), is(nullValue()));
    }
}