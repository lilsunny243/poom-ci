package org.codingmatters.poom.ci.apps.utils;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GenericNotifier {

    private final HttpClientWrapper httpClientWrapper;
    private final String url;
    private final String bearer;

    public GenericNotifier(HttpClientWrapper httpClientWrapper, String url, String bearer) {
        this.httpClientWrapper = httpClientWrapper;
        this.url = url;
        this.bearer = bearer;
    }

    public void notify(String payload) throws IOException {
        System.out.println("notifying...");
        Request.Builder request = new Request.Builder()
                .url(this.url);
        if(this.bearer != null) {
            request = request.addHeader("Authorization", "Bearer " + bearer);
        }
        try(Response response = httpClientWrapper.execute(request
                .post(RequestBody.create(payload.getBytes(StandardCharsets.UTF_8)))
                .build())) {
            if(response.code() != 200 && response.code() != 204) {
                System.err.println("while notifying got status code " + response.code());
                System.err.println("response was : " + response);
                throw new IOException("while notifying got status code " + response.code());
            }
        }
    }
}
