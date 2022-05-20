package org.codingmatters.poom.ci.apps.releaser.task;

import org.codingmatters.poom.ci.apps.releaser.Release;
import org.codingmatters.poom.ci.apps.releaser.RepositoryPipeline;
import org.codingmatters.poom.ci.apps.releaser.Workspace;
import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.git.GithubRepositoryUrlProvider;
import org.codingmatters.poom.ci.apps.releaser.graph.PropagationContext;
import org.codingmatters.poom.ci.apps.releaser.maven.pom.ArtifactCoordinates;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.poom.services.support.date.UTC;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Callable;

public class ReleaseTask implements Callable<ReleaseTaskResult> {
    private final String repository;
    private final String repositoryUrl;
    private final PropagationContext propagationContext;
    private final CommandHelper commandHelper;
    private final PoomCIPipelineAPIClient client;
    private final Workspace workspace;

    public ReleaseTask(String repository, GithubRepositoryUrlProvider githubRepositoryUrlProvider, CommandHelper commandHelper, PoomCIPipelineAPIClient client, Workspace workspace) {
        this(repository, githubRepositoryUrlProvider, new PropagationContext(), commandHelper, client, workspace);
    }
    public ReleaseTask(String repository, GithubRepositoryUrlProvider githubRepositoryUrlProvider, PropagationContext propagationContext, CommandHelper commandHelper, PoomCIPipelineAPIClient client, Workspace workspace) {
        this.repository = repository;
        this.repositoryUrl = githubRepositoryUrlProvider.url(repository);
        this.propagationContext = propagationContext;
        this.commandHelper = commandHelper;
        this.client = client;
        this.workspace = workspace;
    }

    @Override
    public ReleaseTaskResult call() throws Exception {
        LocalDateTime start = UTC.now();

        ArtifactCoordinates releasedCoordinates = new Release(this.repositoryUrl, this.propagationContext, this.commandHelper, this.workspace).initiate();

        RepositoryPipeline pipeline = new RepositoryPipeline(this.repository, "master", this.client);
        Optional<Pipeline> pipe = pipeline.last(start);
        if (!pipe.isPresent()) {
            System.out.println("Waiting for release pipeline to start...");
            do {
                Thread.sleep(2000L);
                pipe = pipeline.last(start);
            } while (!pipe.isPresent());
        }

        System.out.println("waiting for release pipeline to finish...");
        while (!pipe.get().opt().status().run().orElse(Status.Run.PENDING).equals(Status.Run.DONE)) {
            Thread.sleep(2000L);
            pipe = pipeline.last(start);
        }

        if (pipe.get().status().exit().equals(Status.Exit.SUCCESS)) {
            return new ReleaseTaskResult(ReleaseTaskResult.ExitStatus.SUCCESS, String.format("%s released to version %s", this.repository, releasedCoordinates), releasedCoordinates);
        } else {
            System.err.println("release failed !!");
            System.exit(1);
            return new ReleaseTaskResult(ReleaseTaskResult.ExitStatus.FAILURE, String.format("%s release failed", this.repository), null);
        }
    }
}
