package org.codingmatters.poom.ci.apps.releaser.notify;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.command.exception.CommandFailed;
import org.codingmatters.poom.ci.apps.releaser.git.Git;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Arguments;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class Notifier {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(Notifier.class);

    public static final String NOTIFIER_URL = "NOTIFIER_URL";
    public static final String NOTIFIER_BEARER = "NOTIFIER_BEARER";

    public static final String DEFAULT_URL = "https://api.flexio.io/httpin/my/in/5fb7c9b2a6a8c401ab4f4665";
    public static final String DEFAULT_BEARER = "fd62b406-9ccd-4bb5-89a9-3868c395a15e";

    static public Notifier fromArguments(HttpClientWrapper httpClientWrapper, JsonFactory jsonFactory, CommandHelper commandHelper, Arguments arguments) {
        String url = DEFAULT_URL;
        String bearer = DEFAULT_BEARER;

        if(arguments.option("notify-bearer").isPresent()) {
            bearer = arguments.option("notify-bearer").get();
        }
        if(arguments.option("notify-url").isPresent()) {
            url = arguments.option("notify-url").get();
        }
        return new Notifier(httpClientWrapper, jsonFactory, commandHelper, url, bearer);
    }

    static public Notifier fromEnv(HttpClientWrapper httpClientWrapper, JsonFactory jsonFactory, CommandHelper commandHelper) {
        return new Notifier(httpClientWrapper, jsonFactory, commandHelper,
                Env.optional(NOTIFIER_URL).orElse(new Env.Var(DEFAULT_URL)).asString(),
                Env.optional(NOTIFIER_BEARER).orElse(new Env.Var(DEFAULT_BEARER)).asString()
        );
    }

    private final HttpClientWrapper httpClientWrapper;
    private final JsonFactory jsonFactory;
    private final CommandHelper commandHelper;
    private final String url;
    private final String bearer;

    public Notifier(HttpClientWrapper httpClientWrapper, JsonFactory jsonFactory, CommandHelper commandHelper, String url, String bearer) {
        this.httpClientWrapper = httpClientWrapper;
        this.jsonFactory = jsonFactory;
        this.commandHelper = commandHelper;
        this.url = url;
        this.bearer = bearer;
    }

    public void notify(String action, String stage, String message) throws IOException {
        System.out.println("notifying...");

        Git git = new Git(new File("/tmp"), commandHelper);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        payload.put("stage", stage);
        payload.put("message", message);
        try {
            payload.put("username", git.username());
            payload.put("email", git.email());
        } catch (CommandFailed e) {
            log.warn("failed getting username / email", e);
        }
        try(Response response = httpClientWrapper.execute(new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + bearer)
                .post(RequestBody.create(new ObjectMapper(jsonFactory).writeValueAsBytes(payload)))
                .build())) {
            if(response.code() != 200 && response.code() != 204) {
                System.err.println("while notifying got status code " + response.code());
                System.err.println("response was : " + response);
            }
        }
    }

    public void notifyError(String action, String stage, Exception e) {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(); PrintStream stream = new PrintStream(out)) {
            e.printStackTrace(stream);
            stream.flush();
            stream.close();
            this.notify(action, stage, out.toString());
        } catch (IOException ioException) {
            log.warn("error notifying error...", ioException);
        }
    }
}
