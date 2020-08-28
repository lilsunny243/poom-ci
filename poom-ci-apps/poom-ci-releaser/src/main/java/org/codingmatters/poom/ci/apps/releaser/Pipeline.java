package org.codingmatters.poom.ci.apps.releaser;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.pipeline.api.PipelinesGetRequest;
import org.codingmatters.poom.ci.pipeline.api.PipelinesGetResponse;
import org.codingmatters.poom.ci.pipeline.api.ValueList;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIRequesterClient;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

import java.io.IOException;

public class Pipeline {
    private final PoomCIPipelineAPIClient client;

    public Pipeline(PoomCIPipelineAPIClient client) {
        this.client = client;
    }

    public static void main(String[] args) {
        String pipelineUrl = "https://pipelines.ci.flexio.io/pipelines";
        System.out.printf("Looking up pipelines at : %s\n", pipelineUrl);
        JsonFactory jsonFactory = new JsonFactory();
        PoomCIPipelineAPIClient client = new PoomCIPipelineAPIRequesterClient(
                new OkHttpRequesterFactory(OkHttpClientWrapper.build(), () -> pipelineUrl),
                jsonFactory,
                pipelineUrl
        );

        try {
            PipelinesGetResponse response = client.pipelines().get(PipelinesGetRequest.builder()
                    .range("0-99")
                    .filter(String.format(
                            "trigger.checkoutSpec == %s && (status.run == 'RUNNING' || status.run == 'RUNNING')",
                            ""
                    ))
                    .orderBy("")
                    .build());
            System.out.println(response);
            if(response.opt().status200().isPresent() || response.opt().status206().isPresent()) {
                ValueList<org.codingmatters.poom.ci.pipeline.api.types.Pipeline> pipelines = response.opt().status200().payload()
                        .orElseGet(() -> response.opt().status206().payload()
                                .orElseGet(() -> new ValueList.Builder<org.codingmatters.poom.ci.pipeline.api.types.Pipeline>().build()));

                for (org.codingmatters.poom.ci.pipeline.api.types.Pipeline pipeline : pipelines) {
                    System.out.println(pipeline);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        new Pipeline(client);
    }

}
