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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReleaseGraphTask extends AbstractGraphTask implements Callable<GraphTaskResult> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ReleaseGraphTask.class);

    public ReleaseGraphTask(
            List<RepositoryGraphDescriptor> descriptorList,
            CommandHelper commandHelper,
            PoomCIPipelineAPIClient client,
            Workspace workspace,
            Notifier notifier,
            GithubRepositoryUrlProvider githubRepositoryUrlProvider,
            GraphTaskListener graphTaskListener) {
        super(descriptorList, commandHelper, client, workspace, notifier, githubRepositoryUrlProvider, graphTaskListener);
    }

    @Override
    public GraphTaskResult call() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            log.info("starting release-graph for {}", this.descriptorList);
            notifier.notify("release-graph", "START", this.formattedRepositoryList(descriptorList));
            GraphWalker.WalkerTaskProvider walkerTaskProvider = (repository, context) -> new ReleaseTask(repository, githubRepositoryUrlProvider, context, commandHelper, client, workspace);

            PropagationContext propagationContext = new PropagationContext();
            for (RepositoryGraphDescriptor descriptor : descriptorList) {
                walkGraph(descriptor, propagationContext, pool, walkerTaskProvider);
            }

            notifier.notify("release-graph", "DONE", propagationContext.text());
            return new GraphTaskResult(ReleaseTaskResult.ExitStatus.SUCCESS, "Finished releasing graphs", propagationContext);
        } finally {
            pool.shutdownNow();
            log.info("release-graph for {} done", this.descriptorList);
        }
    }
}
