package org.codingmatters.poom.ci.utilities.upstream;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.dependency.api.ValueList;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.client.PoomCIDependencyAPIClient;
import org.codingmatters.poom.ci.dependency.client.PoomCIDependencyAPIRequesterClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIRequesterClient;
import org.codingmatters.poom.ci.triggers.UpstreamBuild;
import org.codingmatters.poom.ci.triggers.upstreambuild.Downstream;
import org.codingmatters.poom.ci.triggers.upstreambuild.Upstream;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class UpstreamBuildTriggerer {

    public static final int TRIES = 5;

    public static void main(String[] args) {
        if(args.length < 5) {
            throw new RuntimeException("usage : <pipeline base url> <dependencies base url> <repository id> <repository name> <checkout spec>");
        }

        JsonFactory jsonFactory = new JsonFactory();

        String pipelineApiUrl = args[0];
        PoomCIPipelineAPIClient pipelineAPIClient = new PoomCIPipelineAPIRequesterClient(
                new OkHttpRequesterFactory(OkHttpClientWrapper.build(), () -> pipelineApiUrl),
                jsonFactory,
                pipelineApiUrl
        );
        String dependencyApiUrl = args[1];
        PoomCIDependencyAPIClient dependencyAPIClient = new PoomCIDependencyAPIRequesterClient(
                new OkHttpRequesterFactory(OkHttpClientWrapper.build(), () -> dependencyApiUrl),
                jsonFactory,
                dependencyApiUrl
        );

        String repositoryId = args[2];
        String repositoryName = args[3];
        String checkoutSpec = args[4];

        Upstream upstream = Upstream.builder()
                .id(repositoryId)
                .name(repositoryName)
                .checkoutSpec(checkoutSpec)
                .build();

        ValueList<Repository> downstreamRepositories;
        try {
            downstreamRepositories = retry(() -> dependencyAPIClient.repositories().repository().repositoryDownstreamRepositories().repositoryJustNextDownstreamRepositories()
                    .get(req -> req.repositoryId(upstream.id()))
                    .opt().status200().payload()
                    .orElseThrow(() -> new RuntimeException("failed getting just next downstream repositories for " + upstream.id() + ". unexpected response from dependecy service")));
        } catch (IOException e) {
            throw new RuntimeException("failed getting just next downstream repositories for " + upstream.id(), e);
        }

        List<Downstream> downstreams = downstreamRepositories.stream().map(repository -> Downstream.builder()
                .id(repository.id())
                .name(repository.name())
                .checkoutSpec(repository.checkoutSpec())
                .build()).collect(Collectors.toList());

        triggerDownstreams(pipelineApiUrl, pipelineAPIClient, upstream, downstreams);

    }
    private static void triggerDownstreams(String pipelineApiUrl, PoomCIPipelineAPIClient pipelineAPIClient, Upstream upstream, Iterable<Downstream> downstreams) {
        for (Downstream downstream : downstreams) {
            try {
                UpstreamBuild upstreamBuild = UpstreamBuild.builder()
                        .upstream(upstream)
                        .downstream(downstream)
                        .build();
                System.out.println("triggering downstream " + downstream);
                retry(() -> pipelineAPIClient.triggers().upstreamBuildTriggers().post(req -> req.payload(upstreamBuild)).opt().status201().orElseThrow(() -> new RuntimeException("failed triggering downstream build")));
                System.out.println("done triggering downstream " + downstream);
            } catch (IOException e) {
                throw new RuntimeException("failed connecting to pipeline api at " + pipelineApiUrl, e);
            }
        }
    }

    static private <T> T retry(Tryable<T> tryable) throws IOException {
        int remainingTries = TRIES;
        IOException lastException;
        do {
            remainingTries--;
            try {
                return tryable.tryThat();
            } catch (IOException e) {
                lastException = e;
                try {
                    Thread.sleep(2 * 1000);
                } catch (InterruptedException e1) {}
            }
        } while(remainingTries > 0);

        throw new IOException(TRIES + " failures, returning last", lastException);
    }

    interface Tryable<T> {
        T tryThat() throws IOException;
    }
}
