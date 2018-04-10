package org.codingmatters.poom.ci.github.webhook;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Undertow;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.ci.triggers.json.GithubPushEventReader;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.api.ResponseDelegate;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Map;

public class Explore implements Processor {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    public static void main(String[] args) {

        Undertow server = Undertow.builder()
                .addHttpListener(6543, "localhost")
                .setHandler(new CdmHttpUndertowHandler(new Explore()))
                .build();
        server.start();


        try {
            System.out.println("press any key to stop server");
            System.in.read();
            System.out.println("really stop ?");
            System.in.read();
            server.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonFactory jsonFactory = new JsonFactory();

    @Override
    public void process(RequestDelegate request, ResponseDelegate response) throws IOException {
        String token = "9a167bbea7e96c86fd87ae254f646cbea050ef63";


        System.out.printf("headers      : %s\n", request.headers());
        System.out.printf("contentType  : %s\n", request.contentType());
        System.out.printf("method       : %s\n", request.method());
        System.out.printf("path         : %s\n", request.path());
        System.out.printf("query params : %s\n", request.queryParameters());

        String payload;
        try(Reader reader = new InputStreamReader(request.payload())) {
            StringBuilder payloadBuilder = new StringBuilder();
            char[] buffer = new char[1024];
            for(int read = reader.read(buffer) ; read != -1 ; read = reader.read(buffer)) {
                payloadBuilder.append(buffer, 0, read);
            }
            payload = payloadBuilder.toString();
        }

        String signature = null;
        try {
            signature = this.signature(token, payload);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            System.err.println("failed signing payload with secret token");
            e.printStackTrace();
        }

        System.out.println(signature);
        System.out.printf("signature    : sha1=%s\n", signature);
        System.out.printf("X-Hub-Signat : %s\n", request.headers().get("X-Hub-Signature").get(0));

        System.out.printf("payload      : \n%s\n---------\n\n", payload);

        Map payloadMap = this.mapper.readValue(payload, Map.class);
        for (Object key : payloadMap.keySet()) {
            System.out.printf("  %s : %s\n", key, payloadMap.get(key));
        }


        if("push".equals(request.headers().get("X-GitHub-Event").get(0))) {
            try(JsonParser parser = this.jsonFactory.createParser(payload)) {
                GithubPushEvent event = new GithubPushEventReader().read(parser);
                System.out.printf("event        : \n%s\n---------\n\n", event);

                System.out.printf("clone url : %s\n", event.repository().clone_url());
                System.out.printf("coordinates : refs=%s - at=%s", event.ref(), event.after());
            }
        }

        response.contenType("text/plain")
                .payload("Thanks !".getBytes());
    }

    private String signature(String token, String payload) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec signingKey = new SecretKeySpec(token.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);
        return this.toHexString(mac.doFinal(payload.getBytes()));
    }


    private static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
