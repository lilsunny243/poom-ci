package org.codingmatters.poom.ci.apps.releaser.task;

import org.codingmatters.poom.ci.apps.releaser.Release;
import org.codingmatters.poom.ci.apps.releaser.RepositoryPipeline;
import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.command.exception.CommandFailed;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.services.support.date.UTC;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Callable;

public class ReleaseTask implements Callable<TaskResult> {
    private final String repository;
    private final String repositoryUrl;
    private final CommandHelper commandHelper;
    private final PoomCIPipelineAPIClient client;

    public ReleaseTask(String repository, CommandHelper commandHelper, PoomCIPipelineAPIClient client) {
        this.repository = repository;
        this.repositoryUrl = String.format("git@github.com:%s.git", repository);
        this.commandHelper = commandHelper;
        this.client = client;
    }

    @Override
    public TaskResult call() throws Exception {
        LocalDateTime start = UTC.now();

        String releasedVersion = new Release(this.repositoryUrl, this.commandHelper).initiate();
        System.out.println("waiting for release pipeline to finish...");

        RepositoryPipeline pipeline = new RepositoryPipeline(this.repository, "master", this.client);
        Optional<Pipeline> pipe = pipeline.last(start);
        if (!pipe.isPresent()) {
            System.out.println("Waiting for release pipeline to start...");
            do {
                Thread.sleep(2000L);
                pipe = pipeline.last(start);
            } while (!pipe.isPresent());
        }

        while (!pipe.get().opt().status().run().orElse(Status.Run.PENDING).equals(Status.Run.DONE)) {
            Thread.sleep(2000L);
            pipe = pipeline.last(start);
        }

        if (pipe.get().status().exit().equals(Status.Exit.SUCCESS)) {
            return new TaskResult(TaskResult.ExitStatus.SUCCESS, String.format("%s released to version %s", this.repository, releasedVersion));
        } else {
            System.err.println("relese failed !!");
            System.exit(1);
            return new TaskResult(TaskResult.ExitStatus.FAILURE, String.format("%s release failed", this.repository));
        }
    }
}
