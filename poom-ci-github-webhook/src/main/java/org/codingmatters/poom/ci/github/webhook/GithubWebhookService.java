package org.codingmatters.poom.ci.github.webhook;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import okhttp3.OkHttpClient;
import org.codingmatters.poom.ci.github.webhook.api.GithubWebhookAPIHandlers;
import org.codingmatters.poom.ci.github.webhook.api.service.GithubWebhookAPIProcessor;
import org.codingmatters.poom.ci.github.webhook.handlers.GithubWebhook;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIRequesterClient;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.api.ResponseDelegate;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class GithubWebhookService {

    static private final CategorizedLogger log = CategorizedLogger.getLogger(GithubWebhookService.class);

    static public final String SERVICE_HOST = "SERVICE_HOST";
    static public final String SERVICE_PORT = "SERVICE_PORT";
    static public final String GITHUB_SECRET_TOKEN = "GITHUB_SECRET_TOKEN";
    static public final String PIPELINE_API_URL = "PIPELINE_API_URL";

    private final String token;
    private final PathHandler handlers;
    private final JsonFactory jsonFactory;
    private final int port;

    private Undertow server;
    private final PoomCIPipelineAPIClient pipelineClient;
    private final String host;

    public static void main(String[] args) {
        String host = mandatory(SERVICE_HOST);
        int port = Integer.parseInt(mandatory(SERVICE_PORT));
        String token = mandatory(GITHUB_SECRET_TOKEN);
        String pipelineUrl = mandatory(PIPELINE_API_URL);

        JsonFactory jsonFactory = new JsonFactory();
        PoomCIPipelineAPIClient pipelineClient = new PoomCIPipelineAPIRequesterClient(new OkHttpRequesterFactory(new OkHttpClient()), jsonFactory, pipelineUrl);
        new GithubWebhookService(host, port, token, jsonFactory, pipelineClient).start();

        log.info("started...");

        while(true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                log.info("stopping service");
                System.exit(1);
            }
        }
    }

    private static String mandatory(String envVariableName) {
        String value = System.getenv(envVariableName);
        if(value == null) throw new RuntimeException("must provide mandatory environment variable : " + envVariableName);
        return value;
    }

    public GithubWebhookService(String host, int port, String token, JsonFactory jsonFactory, PoomCIPipelineAPIClient pipelineClient) {
        this.host = host;
        this.port = port;
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
                .addHttpListener(this.port, this.host)
                .setHandler(this.handlers)
                .build();
        this.server.start();
    }

    public void stop() {
        this.server.stop();
    }
}
