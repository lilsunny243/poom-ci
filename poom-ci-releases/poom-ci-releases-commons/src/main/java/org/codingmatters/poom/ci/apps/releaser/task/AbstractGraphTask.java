package org.codingmatters.poom.ci.apps.releaser.task;

import org.codingmatters.poom.ci.apps.releaser.Workspace;
import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.git.GithubRepositoryUrlProvider;
import org.codingmatters.poom.ci.apps.releaser.graph.GraphWalkResult;
import org.codingmatters.poom.ci.apps.releaser.graph.GraphWalker;
import org.codingmatters.poom.ci.apps.releaser.graph.PropagationContext;
import org.codingmatters.poom.ci.apps.releaser.graph.descriptors.RepositoryGraph;
import org.codingmatters.poom.ci.apps.releaser.graph.descriptors.RepositoryGraphDescriptor;
import org.codingmatters.poom.ci.apps.releaser.notify.Notifier;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.services.support.Env;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class AbstractGraphTask {
    protected final List<RepositoryGraphDescriptor> descriptorList;
    protected final CommandHelper commandHelper;
    protected final PoomCIPipelineAPIClient client;
    protected final Workspace workspace;
    protected final Notifier notifier;
    protected final GithubRepositoryUrlProvider githubRepositoryUrlProvider;

    private final GraphTaskListener graphTaskListener;

    public AbstractGraphTask(
            List<RepositoryGraphDescriptor> descriptorList,
            CommandHelper commandHelper,
            PoomCIPipelineAPIClient client,
            Workspace workspace,
            Notifier notifier,
            GithubRepositoryUrlProvider githubRepositoryUrlProvider,
            GraphTaskListener graphTaskListener
    ) {
        this.descriptorList = descriptorList;
        this.commandHelper = commandHelper;
        this.client = client;
        this.workspace = workspace;
        this.notifier = notifier;
        this.githubRepositoryUrlProvider = githubRepositoryUrlProvider;
        this.graphTaskListener = graphTaskListener;
    }

    protected String formattedRepositoryList(List<RepositoryGraphDescriptor> descriptorList) {
        StringBuilder result = new StringBuilder();
        result.append("Repositories :");
        for (RepositoryGraphDescriptor descriptor : descriptorList) {
            appendRepos(result, descriptor.graph());
        }
        return result.toString();
    }

    protected void appendRepos(StringBuilder result, RepositoryGraph graph) {
        if(graph.opt().repositories().isPresent()) {
            for (String repository : graph.repositories()) {
                result.append("\n   - ").append(repository);
            }
        }
        if(graph.opt().then().isPresent()) {
            for (RepositoryGraph repositoryGraph : graph.then()) {
                appendRepos(result, repositoryGraph);
            }
        }
    }

    protected void walkGraph(RepositoryGraphDescriptor descriptor, PropagationContext propagationContext, ExecutorService pool, GraphWalker.WalkerTaskProvider walkerTaskProvider) throws InterruptedException, java.util.concurrent.ExecutionException {
        GraphWalker walker = new GraphWalker(
                descriptor,
                propagationContext,
                walkerTaskProvider,
                pool
        );
        GraphWalkResult result = pool.submit(walker).get();
        if(result.exitStatus().equals(ReleaseTaskResult.ExitStatus.SUCCESS)) {
            this.graphTaskListener.info(result);
        } else {
            this.graphTaskListener.error(result);
        }
    }
}
