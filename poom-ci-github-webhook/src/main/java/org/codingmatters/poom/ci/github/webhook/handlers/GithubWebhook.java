package org.codingmatters.poom.ci.github.webhook.handlers;

import org.codingmatters.poom.ci.github.webhook.api.WebhookPostRequest;
import org.codingmatters.poom.ci.github.webhook.api.WebhookPostResponse;
import org.codingmatters.poom.ci.pipeline.api.optional.OptionalGithubTriggersPostResponse;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.io.IOException;
import java.util.function.Function;

public class GithubWebhook implements Function<WebhookPostRequest, WebhookPostResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(GithubWebhook.class);

    private final PoomCIPipelineAPIClient pipelineClient;

    public GithubWebhook(PoomCIPipelineAPIClient pipelineClient) {
        this.pipelineClient = pipelineClient;
    }

    @Override
    public WebhookPostResponse apply(WebhookPostRequest request) {
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
    }
}
