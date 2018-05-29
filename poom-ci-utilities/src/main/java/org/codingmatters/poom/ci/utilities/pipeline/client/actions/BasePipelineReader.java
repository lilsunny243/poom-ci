package org.codingmatters.poom.ci.utilities.pipeline.client.actions;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIRequesterClient;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

public class BasePipelineReader {
    private final PoomCIPipelineAPIClient client;

    public BasePipelineReader(String baseUrl) {
        this.client = new PoomCIPipelineAPIRequesterClient(
                new OkHttpRequesterFactory(OkHttpClientWrapper.build()),
                new JsonFactory(),
                baseUrl
        );
    }

    public PoomCIPipelineAPIClient client() {
        return client;
    }
}
