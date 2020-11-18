package org.codingmatters.poom.ci.apps.releaser.graph;

import org.codingmatters.poom.ci.apps.releaser.graph.descriptors.RepositoryGraphDescriptor;
import org.codingmatters.poom.ci.apps.releaser.maven.pom.ArtifactCoordinates;
import org.codingmatters.poom.ci.apps.releaser.task.ReleaseTask;
import org.codingmatters.poom.ci.apps.releaser.task.ReleaseTaskResult;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public class GraphWalkerTest {

    private final List<String> repos = Collections.synchronizedList(new LinkedList<>());

    @Test
    public void given__when__then() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        RepositoryGraphDescriptor repositoryGraph = RepositoryGraphDescriptor.fromYaml(Thread.currentThread().getContextClassLoader().getResourceAsStream("graphs/a-graph.yml"));
        GraphWalker walker = new GraphWalker(
                repositoryGraph,
                this::walkerTask,
                pool
        );

        GraphWalkResult result = pool.submit(walker).get();
        System.out.println(result);
        System.out.println(this.repos);
    }

    private Callable<ReleaseTaskResult> walkerTask(String repository, PropagationContext propagationContext) {
        return new Callable<ReleaseTaskResult>() {
            @Override
            public ReleaseTaskResult call() throws Exception {
                Thread.sleep(500);
                repos.add(repository);
                return new ReleaseTaskResult(ReleaseTaskResult.ExitStatus.SUCCESS, repository, new ArtifactCoordinates(repository, repository, "1.2.3"));
            }
        };
    }
}