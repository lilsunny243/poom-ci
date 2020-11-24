package org.codingmatters.poom.ci.apps.releaser.graph;

import org.codingmatters.poom.ci.apps.releaser.graph.descriptors.RepositoryGraph;
import org.codingmatters.poom.ci.apps.releaser.graph.descriptors.RepositoryGraphDescriptor;
import org.codingmatters.poom.ci.apps.releaser.maven.pom.ArtifactCoordinates;
import org.codingmatters.poom.ci.apps.releaser.task.ReleaseTaskResult;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class GraphWalkerTest {

    private final List<String> repos = Collections.synchronizedList(new LinkedList<>());

    @Test
    public void whenGraphIsDeepWithParrallelBranch__thenAllNodesAreVisited() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        RepositoryGraphDescriptor repositoryGraph = RepositoryGraphDescriptor.fromYaml(Thread.currentThread().getContextClassLoader().getResourceAsStream("graphs/a-graph.yml"));
        GraphWalker walker = new GraphWalker(
                repositoryGraph,
                new PropagationContext(),
                this::walkerTask,
                pool
        );

        GraphWalkResult result = pool.submit(walker).get();
        System.out.println(result);
        System.out.println(this.repos);

        assertThat(this.repos, containsInAnyOrder("url-1","url-2" , "url-3" , "url-7" , "url-4" , "url-6" , "url-5"));
    }
    @Test
    public void whenGraphRootDoesntHaveRepositories__thenThenBranchAreWalked() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        RepositoryGraphDescriptor repositoryGraph = new RepositoryGraphDescriptor(RepositoryGraph.builder()
                .then(
                        RepositoryGraph.builder().repositories("url-1").build(),
                        RepositoryGraph.builder().repositories("url-2").build()
                )
                .build());
        GraphWalker walker = new GraphWalker(
                repositoryGraph,
                new PropagationContext(),
                this::walkerTask,
                pool
        );

        GraphWalkResult result = pool.submit(walker).get();
        System.out.println(result);
        System.out.println(this.repos);

        assertThat(this.repos, containsInAnyOrder("url-1", "url-2"));
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