package org.codingmatters.poom.ci.apps.releaser.graph.descriptors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class RepositoryGraphDescriptorTest {
    private RepositoryGraphDescriptor graphDescriptor;

    @Before
    public void setUp() throws Exception {
        this.graphDescriptor = RepositoryGraphDescriptor.fromYaml(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("graphs/a-graph.yml")
        );
    }

    @Test
    public void whenStartsAtFirstRootRepo__thenGraphUnchanged() throws Exception {
        RepositoryGraphDescriptor actual = graphDescriptor.subgraph("url-1");

        assertThat(actual, is(graphDescriptor));
    }

    @Test
    public void whenStartsAtSecondRootRepo__thenGraphOnlyBeginningOfRootRepoIsByPassed() throws Exception {
        RepositoryGraphDescriptor actual = graphDescriptor.subgraph("url-2");

        assertThat(actual, is(new RepositoryGraphDescriptor(RepositoryGraph.builder()
                .repositories("url-2")
                .then(graphDescriptor.graph().then())
                .build())));
    }

    @Test
    public void whenStartsAtFirstRepoOfOneOfThenGraph__thenTheThenGraphIsReturned() throws Exception {
        RepositoryGraphDescriptor actual = graphDescriptor.subgraph("url-3");

        assertThat(actual, is(new RepositoryGraphDescriptor(RepositoryGraph.builder()
                .repositories("url-3")
                .then(
                        RepositoryGraph.builder().repositories("url-4", "url-5").build(),
                        RepositoryGraph.builder().repositories("url-6").build()
                )
                .build())));
    }

    @Test
    public void whenStartsAtFirstRepoOfLastThen__thenLastThenGrapIsReturned() throws Exception {
        RepositoryGraphDescriptor actual = graphDescriptor.subgraph("url-7");

        assertThat(actual, is(new RepositoryGraphDescriptor(RepositoryGraph.builder()
                .repositories("url-7")
                .build()))
        );
    }
}