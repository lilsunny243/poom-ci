package org.codingmatters.poom.ci.github.webhook;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import org.codingmatters.poom.ci.github.webhook.api.GithubWebhookAPIHandlers;
import org.codingmatters.poom.ci.github.webhook.api.service.GithubWebhookAPIProcessor;
import org.codingmatters.poom.ci.github.webhook.handlers.GithubWebhook;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

public class GithubWebhookService {

    private final String token;
    private final PathHandler handlers;
    private final JsonFactory jsonFactory;

    private Undertow server;
    private final PoomCIPipelineAPIClient pipelineClient;

    public GithubWebhookService(String token, JsonFactory jsonFactory, PoomCIPipelineAPIClient pipelineClient) {
        this.token = token;
        this.jsonFactory = jsonFactory;
        this.pipelineClient = pipelineClient;
        this.handlers = Handlers.path();

        this.handlers.addExactPath(
                "/github/webhook",
                new CdmHttpUndertowHandler(
                        new GithubWebhookGuard(
                                new GithubWebhookAPIProcessor(
                                        "/github/webhook",
                                        this.jsonFactory,
                                        this.webhookHandlers()),
                                this.token)
                ));
    }

    private GithubWebhookAPIHandlers webhookHandlers() {
        return new GithubWebhookAPIHandlers.Builder()
                .webhookPostHandler(new GithubWebhook(this.pipelineClient))
                .build();
    }

    public void start() {
        this.server = Undertow.builder()
                .addHttpListener(6543, "localhost")
                .setHandler(this.handlers)
                .build();
        this.server.start();
    }

    public void stop() {
        this.server.stop();
    }
}
