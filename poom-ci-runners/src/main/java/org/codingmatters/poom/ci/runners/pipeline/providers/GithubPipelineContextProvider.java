package org.codingmatters.poom.ci.runners.pipeline.providers;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.pipeline.api.GithubTriggerGetResponse;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.runners.pipeline.PipelineVariables;
import org.codingmatters.poom.ci.runners.pipeline.providers.gh.AbstractGitHubPipelineContextProvider;
import org.codingmatters.poom.ci.runners.pipeline.providers.gh.Ref;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;

import java.io.IOException;

public class GithubPipelineContextProvider extends AbstractGitHubPipelineContextProvider {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(GithubPipelineContextProvider.class);

    public GithubPipelineContextProvider(PoomCIPipelineAPIClient pipelineAPIClient, YAMLFactory yamlFactory) {
        super(pipelineAPIClient, yamlFactory);
    }

    @Override
    protected PipelineVariables createVariables(String pipelineId, PipelineTrigger trigger) throws ProcessingException {
        GithubPushEvent event = this.retrieveEvent(trigger);
        String repositoryId = String.format(
                "%s-%s",
                event.repository().name().replaceAll("/", "-"),
                this.branchFromRef(event)
        );
        return PipelineVariables.builder()
                        .pipelineId(pipelineId)

                        .repositoryId(repositoryId)
                        .repository(event.repository().full_name())
                        .repositoryUrl(this.repositoryUrl(event))

                        .branch(this.branchFromRef(event))
                        .changeset(event.after())

                        .checkoutSpec(String.format("git|%s|%s", this.repositoryUrl(event), this.branchFromRef(event)))

                        .build();
    }

    private GithubPushEvent retrieveEvent(PipelineTrigger trigger) throws ProcessingException {
        try {
            GithubTriggerGetResponse response = this.pipelineAPIClient().triggers().githubTriggers().githubTrigger().get(req -> req.triggerId(trigger.triggerId()));

            return response.opt().status200().payload()
                    .orElseThrow(() -> {
                        String token = log.tokenized().error("while retrieving github push event, received unexpected response : {}", response);
                        return new ProcessingException("error getting pipeline trigger, see logs with token " + token);
                    });
        } catch (IOException e) {
            throw new ProcessingException("failed accessing pipeline api");
        }
    }

    private String repositoryUrl(GithubPushEvent event) {
        String url = event.repository().clone_url();
        if(Env.optional("GH_PIPE_USE_SSH").orElse(new Env.Var("false")).asString().equals("true")) {
            url = event.repository().ssh_url();
        }
        return url;
    }

    private String branchFromRef(GithubPushEvent event) {
        return new Ref(event.ref()).branch();
    }
}
