package org.codingmatters.poom.ci.apps.releaser.graph;

import org.codingmatters.poom.ci.apps.releaser.graph.descriptors.RepositoryGraph;
import org.codingmatters.poom.ci.apps.releaser.graph.descriptors.RepositoryGraphDescriptor;
import org.codingmatters.poom.ci.apps.releaser.task.ReleaseTaskResult;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class GraphWalker implements Callable<GraphWalkResult> {
    public interface WalkerTaskProvider {
        Callable<ReleaseTaskResult> create(String repository, PropagationContext context);
    }

    private final RepositoryGraphDescriptor repositoryGraph;
    private final PropagationContext propagationContext;
    private final WalkerTaskProvider walkerTaskProvider;
    private final ExecutorService pool;

    public GraphWalker(RepositoryGraphDescriptor repositoryGraph, PropagationContext propagationContext, WalkerTaskProvider walkerTaskProvider, ExecutorService pool) {
        this.repositoryGraph = repositoryGraph;
        this.propagationContext = propagationContext;
        this.walkerTaskProvider = walkerTaskProvider;
        this.pool = pool;
    }

    @Override
    public GraphWalkResult call() throws Exception {
        RepositoryGraph root = this.repositoryGraph.graph();
        Optional<GraphWalkResult> result = this.walk(root);
        return result.orElseGet(() -> new GraphWalkResult(ReleaseTaskResult.ExitStatus.SUCCESS, "graph processed"));
    }

    private Optional<GraphWalkResult> walk(RepositoryGraph root) throws ExecutionException, InterruptedException {
        if(root.opt().repositories().isPresent()) {
            for (String repository : root.repositories()) {
                ReleaseTaskResult result = this.pool.submit(this.walkerTaskProvider.create(repository, this.propagationContext)).get();
                if (result.exitStatus().equals(ReleaseTaskResult.ExitStatus.SUCCESS)) {
                    this.propagationContext.addPropagatedArtifact(result.releasedVersion());
                } else {
                    return Optional.of(new GraphWalkResult(ReleaseTaskResult.ExitStatus.FAILURE, result.message()));
                }
            }
        }

        List<Future<GraphWalkResult>> subtasks = new LinkedList<>();
        if(root.opt().then().isPresent()) {
            for (RepositoryGraph graph : root.then()) {
                subtasks.add(this.pool.submit(new GraphWalker(new RepositoryGraphDescriptor(graph), this.propagationContext, this.walkerTaskProvider, this.pool)));
            }
        }

        boolean failures = false;
        StringBuilder failureMessages = new StringBuilder();
        for (Future<GraphWalkResult> subtask : subtasks) {
            GraphWalkResult result = subtask.get();
            if(! result.exitStatus().equals(ReleaseTaskResult.ExitStatus.SUCCESS    )) {
                failures = true;
                failureMessages.append("- ").append(result.message()).append("\n");
            }
        }
        if(failures) {
            return Optional.of(new GraphWalkResult(ReleaseTaskResult.ExitStatus.FAILURE, failureMessages.toString()));
        } else {
            return Optional.empty();
        }
    }
}
