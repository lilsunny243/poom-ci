package org.codingmatters.poom.ci.apps.releaser;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.pipeline.api.PipelinesGetRequest;
import org.codingmatters.poom.ci.pipeline.api.PipelinesGetResponse;
import org.codingmatters.poom.ci.pipeline.api.ValueList;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIRequesterClient;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.ResponseDelegate;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

public class RepositoryPipeline {
    private final String repo;
    private final String branch;
    private final PoomCIPipelineAPIClient client;

    public RepositoryPipeline(String repo, String branch, PoomCIPipelineAPIClient client) {
        this.repo = repo;
        this.branch = branch;
        this.client = client;
    }

    public Optional<Pipeline> last(LocalDateTime after) throws IOException {
        int tries = 0;
        IOException lastException = null;
        PipelinesGetResponse response = null;
        do {
            try {
                tries++;
                String pipelineFilter = String.format(
                        "trigger.checkoutSpec == 'git|git@github.com:%s.git|%s' && status.triggered >= %3$tY-%3$tm-%3$tdT%3$tH:%3$tM:00.000",
                        this.repo, this.branch, after
                );
                response = this.client.pipelines().get(PipelinesGetRequest.builder()
                        .filter(pipelineFilter)
                        .orderBy("status.triggered desc")
                        .build());
                if (response.opt().status200().isPresent() || response.opt().status206().isPresent()) {
                    PipelinesGetResponse r = response;
                    ValueList<Pipeline> pipelines = r.opt().status200().payload()
                            .orElseGet(() -> r.opt().status206().payload()
                                    .orElseGet(() -> new ValueList.Builder<Pipeline>().build()));
                    return pipelines.isEmpty() ? Optional.empty() : Optional.of(pipelines.get(0));
                }
            } catch (IOException e) {
                lastException = e;
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                throw new IOException("error sleeping for retry", e);
            }
        } while (tries < 10);

        if(lastException == null) {
            throw new IOException("failed to retrieve pipeline, response was : " + response);
        } else {
            throw new IOException("failed to retrieve pipeline, response was : " + response, lastException);
        }
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
            Optional<Pipeline> pipeline = new RepositoryPipeline("flexiooss/poom-ci", "develop", client).last(LocalDateTime.of(2020, 8, 28, 13, 00));
            if(pipeline.isPresent()) {
                System.out.println("found " + pipeline);
            } else {
                System.out.println("no pipeline found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
