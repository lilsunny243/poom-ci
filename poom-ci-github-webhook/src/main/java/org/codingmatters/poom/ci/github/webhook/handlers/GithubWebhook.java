package org.codingmatters.poom.ci.github.webhook.handlers;

import org.codingmatters.poom.ci.github.webhook.api.WebhookPostRequest;
import org.codingmatters.poom.ci.github.webhook.api.WebhookPostResponse;
import org.codingmatters.poom.ci.pipeline.api.optional.OptionalGithubTriggersPostResponse;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.triggers.GHRepository;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.io.IOException;
import java.util.Set;
import java.util.function.Function;

public class GithubWebhook implements Function<WebhookPostRequest, WebhookPostResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(GithubWebhook.class);

    private final Set<String> repositoryWhiteList;

    private final PoomCIPipelineAPIClient pipelineClient;

    public GithubWebhook(Set<String> repositoryWhiteList, PoomCIPipelineAPIClient pipelineClient) {
        this.repositoryWhiteList = repositoryWhiteList;
        this.pipelineClient = pipelineClient;
    }

    @Override
    public WebhookPostResponse apply(WebhookPostRequest request) {
        if(this.isWhiteListed(request.payload().repository())) {
            try {
                OptionalGithubTriggersPostResponse pipelineResponse = this.pipelineClient.triggers().githubTriggers().post(req -> req.payload(request.payload())).opt();

                if (pipelineResponse.status201().isPresent()) {
                    log.audit().info("successfully forwarded github event to pipeline api : {}", request);
                    return WebhookPostResponse.builder()
                            .status200(status -> status.payload("Thanks!"))
                            .build();
                } else {
                    return WebhookPostResponse.builder()
                            .status500(status -> status.payload(String.format(
                                    "error pushing event, see logs (token=%s)",
                                    log.tokenized().error("pipeline api refused event (event={} ; response={})", request, pipelineResponse.get()))
                            ))
                            .build();
                }
            } catch (IOException e) {
                return WebhookPostResponse.builder()
                        .status500(status -> status.payload(String.format(
                                "error pushing event, see logs (token=%s)",
                                log.tokenized().error("error calling pipeline api", e))
                        )).build();
            }
        } else {
            log.audit().info("received request from github, but repository is not white listed, not forwarding : {}", request.payload().repository());
            return WebhookPostResponse.builder()
                    .status202(status -> status.payload("I won't process that, but, thanks!"))
                    .build();
        }
    }

    private boolean isWhiteListed(GHRepository repository) {
        return this.repositoryWhiteList.contains(repository.name()) || this.repositoryWhiteList.contains(repository.full_name());
    }
}
