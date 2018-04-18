package org.codingmatters.poom.ci.github.webhook;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import org.codingmatters.poom.ci.github.webhook.api.GithubWebhookAPIHandlers;
import org.codingmatters.poom.ci.github.webhook.api.service.GithubWebhookAPIProcessor;
import org.codingmatters.poom.ci.github.webhook.handlers.GithubWebhook;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.api.ResponseDelegate;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class GithubWebhookService {

    static private final CategorizedLogger log = CategorizedLogger.getLogger(GithubWebhookService.class);

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
                                new GithubEventFilter(this::notImplementedEvent)
                                        .with("push",
                                                new GithubWebhookAPIProcessor(
                                                        "/github/webhook",
                                                        this.jsonFactory,
                                                        this.webhookHandlers())
                                        )
                                        .with("ping", this::pong),
                                this.token)
                ));
    }

    private void pong(RequestDelegate request, ResponseDelegate response) {
        log.audit().info("got a ping, issuing a pong : {}" , this.payloadAsString(request));
        response.status(200);
        response.contenType("text/plain");
        response.payload("pong", "UTF-8");
    }

    private void notImplementedEvent(RequestDelegate request, ResponseDelegate response) {
        String logToken = log.audit().tokenized().info("event {} not processed ; {}",
                request.headers().get(GithubEventFilter.EVENT_HEADER),
                this.payloadAsString(request)
                );

        response.status(501);
        response.contenType("text/plain");
        response.payload(String.format("event not implemented, see logs (token=%s)", logToken), "UTF-8");
    }

    private String payloadAsString(RequestDelegate request) {
        try {
            try(Reader payload = new InputStreamReader(request.payload())) {
                StringBuilder result = new StringBuilder();
                char[] buffer = new char[1024];
                for(int read = payload.read(buffer) ; read != -1 ; read = payload.read(buffer)) {
                    result.append(buffer, 0, read);
                }
                return result.toString();
            }
        } catch (IOException e) {
            return String.format(
                    "failed reading payload see logs (token=%s)",
                    log.tokenized().error("error reading request payload", e)
            );
        }
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
