package org.codingmatters.poom.ci.github.webhook;

import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.api.ResponseDelegate;
import org.codingmatters.rest.api.processors.GuardedProcessor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class GithubWebhookGuard extends GuardedProcessor {

    static private final CategorizedLogger log = CategorizedLogger.getLogger(GithubWebhookGuard.class);
    static private final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    private final String token;

    public GithubWebhookGuard(Processor guarded, String token) {
        super(guarded);
        this.token = token;
    }

    @Override
    protected boolean passed(RequestDelegate request, ResponseDelegate response) throws IOException {
        try {
            String signature = this.signature(this.readPayload(request));
            if(request.headers().get("X-Hub-Signature") != null && ! request.headers().get("X-Hub-Signature").isEmpty()) {
                if(request.headers().get("X-Hub-Signature").get(0).equals("sha1=" + signature)) {
                    return true;
                } else {
                    this.errorResponse(response, log.tokenized().error("signature doesn't match, expected sha1={} but was {}",
                            signature,
                            request.headers().get("X-Hub-Signature").get(0)));
                }
            } else {
                this.errorResponse(response, log.tokenized().error("X-Hub-Signature not provided"));
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            this.errorResponse(response, log.tokenized().error("error verifying signature", e));
        }
        return false;
    }

    private void errorResponse(ResponseDelegate response, String errorToken) {
        response.contenType("text/plain");
        response.status(403);
        response.payload(String.format("signature doesn't match, see logs (%s)", errorToken).getBytes());
    }

    private String readPayload(RequestDelegate request) throws IOException {
        try(Reader reader = new InputStreamReader(request.payload())) {
            StringBuilder payloadBuilder = new StringBuilder();
            char[] buffer = new char[1024];
            for(int read = reader.read(buffer) ; read != -1 ; read = reader.read(buffer)) {
                payloadBuilder.append(buffer, 0, read);
            }
            return payloadBuilder.toString();
        }
    }

    private String signature(String payload) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec signingKey = new SecretKeySpec(this.token.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);
        return this.toHexString(mac.doFinal(payload.getBytes()));
    }


    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
