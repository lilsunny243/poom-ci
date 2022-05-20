package org.codingmatters.poom.ci.apps.releaser.task;

import org.codingmatters.poom.ci.apps.releaser.Workspace;
import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.git.GithubRepositoryUrlProvider;
import org.codingmatters.poom.ci.apps.releaser.graph.GraphWalker;
import org.codingmatters.poom.ci.apps.releaser.graph.PropagationContext;
import org.codingmatters.poom.ci.apps.releaser.graph.descriptors.RepositoryGraphDescriptor;
import org.codingmatters.poom.ci.apps.releaser.notify.Notifier;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PropagateGraphVersionsTask extends AbstractGraphTask implements Callable<GraphTaskResult> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PropagateGraphVersionsTask.class);
    private final String branch;

    public PropagateGraphVersionsTask(List<RepositoryGraphDescriptor> descriptorList, Optional<String> branch, CommandHelper commandHelper, PoomCIPipelineAPIClient client, Workspace workspace, Notifier notifier, GithubRepositoryUrlProvider githubRepositoryUrlProvider) {
        super(descriptorList, commandHelper, client, workspace, notifier, githubRepositoryUrlProvider);
        this.branch = branch.orElse("develop");
    }

    @Override
    public GraphTaskResult call() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            log.info("starting propagate-versions for {}", this.descriptorList);
            notifier.notify("propagate-versions", "START", formattedRepositoryList(descriptorList));
            GraphWalker.WalkerTaskProvider walkerTaskProvider = (repository, context) -> new PropagateVersionsTask(repository, this.githubRepositoryUrlProvider, branch, context, commandHelper, client, workspace);

            PropagationContext propagationContext = new PropagationContext();
            for (RepositoryGraphDescriptor descriptor : descriptorList) {
                walkGraph(descriptor, propagationContext, pool, walkerTaskProvider);
            }

            notifier.notify("propagate-versions", "DONE", propagationContext.text());
            return new GraphTaskResult(ReleaseTaskResult.ExitStatus.SUCCESS, "Finished propagating versions", propagationContext);
        } finally {
            pool.shutdownNow();
            log.info("propagate-versions for {} done", this.descriptorList);
        }
    }
}
