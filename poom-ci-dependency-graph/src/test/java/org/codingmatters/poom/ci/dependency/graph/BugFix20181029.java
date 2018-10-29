package org.codingmatters.poom.ci.dependency.graph;

import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class BugFix20181029 {

    private DependencyGraph graph;

    @Before
    public void setUp() throws Exception {
        File graphml = new File(Thread.currentThread().getContextClassLoader().getResource("bug-first-next-graph.xml").toURI());
        this.graph = new DependencyGraph(graphml);
    }

    @Test
    public void cdmRestHasNoNext() throws IOException {
        Optional<Repository> repo = this.graph.repositoryById("flexiooss-codingmatters-rest-master");

        DownstreamGraph downGraph = this.graph.downstreamGraph(repo.get());
        Repository[] firsts = downGraph.dependencyTreeFirstSteps(repo.get());

        assertThat(firsts, is(not(emptyArray())));
    }

    @Test
    public void given__when__then() {
        Optional<Repository> repo = this.graph.repositoryById("flexiooss-codingmatters-rest-master");
        for (Repository repository : this.graph.downstream(repo.get())) {
            System.out.println("#######" + repository.id());
            for (Module sub : this.graph.produced(repository)) {
                System.out.println("\t\t" + sub.spec());
            }
//            for (Repository sub : this.graph.downstream(repository)) {
//                System.out.println("\t\t" + sub.id());
//            }
        }
    }

    @Test
    public void showDownstream() throws IOException {
        Optional<Repository> repo = this.graph.repositoryById("flexiooss-codingmatters-rest-master");

        System.out.println("########################################################");
        System.out.println("###### downstream");
        for (Repository repository : this.graph.downstream(repo.get())) {
            System.out.println(repository.id());
        }

        System.out.println();
        System.out.println();
        System.out.println("########################################################");
        System.out.println("###### downstream graph");
        for (Repository repository : this.graph.downstreamGraph(repo.get()).direct(repo.get())) {
            System.out.println(repository.id());
        }
    }
}
