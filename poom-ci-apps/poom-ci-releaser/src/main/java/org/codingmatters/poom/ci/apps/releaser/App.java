package org.codingmatters.poom.ci.apps.releaser;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.command.exception.CommandFailed;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIRequesterClient;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

public class App {
    public static void main(String[] args) {
        if(args.length < 1) throw new RuntimeException("Usage : <repository, i.e. flexiooss/poom-ci>");

        CommandHelper commandHelper = new CommandHelper(line -> System.out.println(line), line -> System.err.println(line));
        JsonFactory jsonFactory = new JsonFactory();
        String pipelineUrl = "https://pipelines.ci.flexio.io/pipelines";
        PoomCIPipelineAPIClient client = new PoomCIPipelineAPIRequesterClient(
                new OkHttpRequesterFactory(OkHttpClientWrapper.build(), () -> pipelineUrl),
                jsonFactory,
                pipelineUrl
        );

        String repository = args[0];
        String repositoryUrl = String.format("git@github.com:%s.git", repository);

        try {
            LocalDateTime start = UTC.now();

            String releasedVersion = new Release(repositoryUrl, commandHelper).initiate();
            System.out.println("waiting for release pipeline to finish...");

            RepositoryPipeline pipeline = new RepositoryPipeline(repository, "master", client);
            Optional<Pipeline> pipe = pipeline.last(start);
            if(! pipe.isPresent()) {
                System.out.println("Waiting for release pipeline to start...");
                do {
                    Thread.sleep(2000L);
                    pipe = pipeline.last(start);
                } while (! pipe.isPresent());
            }

            while(! pipe.get().opt().status().run().orElse(Status.Run.PENDING).equals(Status.Run.DONE)) {
                Thread.sleep(2000L);
                pipe = pipeline.last(start);
            }

            if(pipe.get().status().exit().equals(Status.Exit.SUCCESS)) {
                System.out.printf("%s released to version %s\n", repository, releasedVersion);
                System.exit(0);
            } else {
                System.err.println("relese failed !!");
                System.exit(1);
            }

        } catch (CommandFailed | IOException | InterruptedException commandFailed) {
            commandFailed.printStackTrace();
        }

    }
}
