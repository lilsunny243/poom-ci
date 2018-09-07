package org.codingmatters.poom.ci.dependency.graph;

import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MovedRepositoryTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private DependencyGraph graph;

    @Before
    public void setUp() throws Exception {
        File graphml = this.temporaryResource("cyclic-graph.xml");
        this.graph = new DependencyGraph(graphml);
    }

    private File temporaryResource(String resource) throws Exception {
        File tempFile = this.temp.newFile();
        try(
                InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
                OutputStream out = new FileOutputStream(tempFile)
        ) {
            byte[] buffer = new byte[1024];
            for(int read = in.read(buffer) ; read != -1 ; read = in.read(buffer)) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
        return tempFile;
    }

    @Test
    public void givenARepositoryWasMoved__whenRequestingFirstDownstreamsFromNew__thenOldIsReturned() throws Exception {
        Repository source = this.graph.repositoryById("flexiooss-flexio-commons-develop").get();
        assertThat(
                this.graph.downstreamGraph(source).dependencyTreeFirstSteps(source)[0].id(), is("Flexio-corp-flexio-commons-develop")
        );
    }

    @Test
    public void givenARepositoryWasMoved__whenRequestingFirstDownstreamsFromOld__thenNewIsReturned() throws Exception {
        Repository source = this.graph.repositoryById("Flexio-corp-flexio-commons-develop").get();
        assertThat(
                this.graph.downstreamGraph(source).dependencyTreeFirstSteps(source)[0].id(), is("flexiooss-flexio-commons-develop")
        );
    }

    @Test
    public void givenARepositoryWasMoved_andOldOneRemoved__whenRequestingFirstDownstreamsFromNew__thenDownstreamsAreRuturned() throws Exception {
        Repository newRepo = this.graph.repositoryById("flexiooss-flexio-commons-develop").get();
        Repository oldRepo = this.graph.repositoryById("Flexio-corp-flexio-commons-develop").get();

        this.graph.remove(oldRepo);

        assertThat(
                this.graph.downstreamGraph(newRepo).dependencyTreeFirstSteps(newRepo)[0].id(), is("Flexio-corp-flexio-services-develop")
        );


    }
}
