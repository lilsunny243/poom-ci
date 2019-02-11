package org.codingmatters.poom.ci.dependency.graph;

import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.graph.tinkerpop.AbstractTinkerPopRepositoryGraph;
import org.codingmatters.poom.ci.dependency.graph.tinkerpop.TinkerPopDependencyGraph;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DependencyGraphRealSampleTest {

    private TinkerPopDependencyGraph graph;

    @Before
    public void setUp() throws Exception {
        File graphml = new File(Thread.currentThread().getContextClassLoader().getResource("real-graph.xml").toURI());
        this.graph = new TinkerPopDependencyGraph(graphml);
    }

    @Test
    public void showModules() throws IOException {
        for (Repository repository : this.graph.repositories()) {
            System.out.println(repository.id());
            for (Module module : this.graph.produced(repository)) {
                System.out.println(" |__ " + module);
            }
        }
    }

    @Test
    public void showDownstreams() throws IOException {
        for (Repository repository : this.graph.repositories()) {
            System.out.println(repository.id());
            for (Repository downstream : this.graph.downstream(repository)) {
                System.out.println("     ==> " + downstream.id());
            }
        }
    }

    @Test
    public void buildDownstreamGraph() throws IOException {
        Repository root = this.graph.repositoryById("Flexio-corp-flexio-commons-develop").get();
        DownstreamGraph downstreamGraph = this.graph.downstreamGraph(root);
        ((AbstractTinkerPopRepositoryGraph)downstreamGraph).io().writeGraph("/tmp/flexiooss-codingmatters-reflect-unit-master-downstream.graphml");
        System.out.println("######################################");
        System.out.println("######################################");
        System.out.println(root.id());

        System.out.println("######################################");
        for (Repository repository : downstreamGraph.direct(root)) {
            System.out.println(repository.id());
        }

        System.out.println("######################################");
        for (Repository repository : downstreamGraph.dependencyTreeFirstSteps(root)) {
            System.out.println(repository.id());
        }
    }

    @Test
    public void directDownstream() throws Exception{
        Repository root = this.graph.repositoryById("flexiooss-codingmatters-reflect-unit-master").get();
        DownstreamGraph downstreamGraph = this.graph.downstreamGraph(root);

        assertThat(
                Arrays.stream(downstreamGraph.direct(root)).map(Repository::id).collect(Collectors.toList()),
                is(containsInAnyOrder(
            "Flexio-corp-flexio-commons-develop" ,
                    "flexiooss-codingmatters-rest-master" ,
                    "flexiooss-codingmatters-value-objects-master"
                ))
        );
    }

    @Test
    public void dependencyTreeFirstSteps() throws Exception{
        Repository root = this.graph.repositoryById("flexiooss-codingmatters-reflect-unit-master").get();
        DownstreamGraph downstreamGraph = this.graph.downstreamGraph(root);

        assertThat(
                Arrays.stream(downstreamGraph.dependencyTreeFirstSteps(root)).map(Repository::id).collect(Collectors.toList()),
                is(containsInAnyOrder(
                    "flexiooss-codingmatters-value-objects-master"
                ))
        );
    }
}
