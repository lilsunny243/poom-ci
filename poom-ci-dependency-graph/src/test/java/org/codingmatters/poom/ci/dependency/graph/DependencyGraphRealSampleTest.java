package org.codingmatters.poom.ci.dependency.graph;

import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
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

    private DependencyGraph graph;

    @Before
    public void setUp() throws Exception {
        File graphml = new File(Thread.currentThread().getContextClassLoader().getResource("real-graph.xml").toURI());
        this.graph = new DependencyGraph(graphml);
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
        DownstreamGraph downstreamGraph = this.graph.downstreamGraph(this.graph.repositoryById("Flexio-corp-flexio-commons-develop").get());
        downstreamGraph.io().writeGraph("/tmp/flexiooss-codingmatters-reflect-unit-master-downstream.graphml");
        System.out.println("######################################");
        System.out.println("######################################");
        System.out.println(downstreamGraph.root().id());

        System.out.println("######################################");
        for (Repository repository : downstreamGraph.direct(downstreamGraph.root())) {
            System.out.println(repository.id());
        }

        System.out.println("######################################");
        for (Repository repository : downstreamGraph.dependencyTreeFirstSteps(downstreamGraph.root())) {
            System.out.println(repository.id());
        }
    }

    @Test
    public void directDownstream() throws Exception{
        DownstreamGraph downstreamGraph = this.graph.downstreamGraph(this.graph.repositoryById("flexiooss-codingmatters-reflect-unit-master").get());

        assertThat(
                Arrays.stream(downstreamGraph.direct(downstreamGraph.root())).map(Repository::id).collect(Collectors.toList()),
                is(containsInAnyOrder(
            "Flexio-corp-flexio-commons-develop" ,
                    "flexiooss-codingmatters-rest-master" ,
                    "flexiooss-codingmatters-value-objects-master"
                ))
        );
    }

    @Test
    public void dependencyTreeFirstSteps() throws Exception{
        DownstreamGraph downstreamGraph = this.graph.downstreamGraph(this.graph.repositoryById("flexiooss-codingmatters-reflect-unit-master").get());

        assertThat(
                Arrays.stream(downstreamGraph.dependencyTreeFirstSteps(downstreamGraph.root())).map(Repository::id).collect(Collectors.toList()),
                is(containsInAnyOrder(
                    "flexiooss-codingmatters-value-objects-master"
                ))
        );
    }
}
