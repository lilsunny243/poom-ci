package org.codingmatters.poom.ci.dependency.graph;

import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DependencyGraphBackupTest {

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    @Test
    public void givenABackupFileIsProvided__whenWritingModule__thenGraphIsBackedUp() throws Exception {
        File backupFile = this.dir.newFile();
        DependencyGraph graph = new DependencyGraph(backupFile);
        graph.add(Module.builder().spec("module:name").version("0.0.1-SNAPSHOT").build());

        DependencyGraph backup = new DependencyGraph(backupFile);
        assertThat(backup.modules(), is(graph.modules()));
    }

    @Test
    public void givenABackupFileIsProvided__whenWritingRepository__thenGraphIsBackedUp() throws Exception {
        File backupFile = this.dir.newFile();
        DependencyGraph graph = new DependencyGraph(backupFile);
        graph.add(Repository.builder().id("repo").name("repo").checkoutSpec("checkout/spec").build());

        DependencyGraph backup = new DependencyGraph(backupFile);
        assertThat(backup.repositories(), is(graph.repositories()));
    }

    @Test
    public void givenABackupFileIsProvided__whenWritingAProducedModule__thenGraphIsBackedUp() throws Exception {
        File backupFile = this.dir.newFile();
        DependencyGraph graph = new DependencyGraph(backupFile);
        graph.produces(
                Repository.builder().id("repo").name("repo").checkoutSpec("checkout/spec").build(),
                Module.builder().spec("module:name").version("0.0.1-SNAPSHOT").build());

        DependencyGraph backup = new DependencyGraph(backupFile);
        assertThat(backup.produced(Repository.builder().id("repo").name("repo").checkoutSpec("checkout/spec").build()),
                is(graph.produced(Repository.builder().id("repo").name("repo").checkoutSpec("checkout/spec").build())));
    }

    @Test
    public void givenABackupFileIsProvided__whenWritingADependencyModule__thenGraphIsBackedUp() throws Exception {
        File backupFile = this.dir.newFile();
        DependencyGraph graph = new DependencyGraph(backupFile);
        graph.dependsOn(
                Repository.builder().id("repo").name("repo").checkoutSpec("checkout/spec").build(),
                Module.builder().spec("module:name").version("0.0.1-SNAPSHOT").build());

        DependencyGraph backup = new DependencyGraph(backupFile);
        assertThat(backup.depending(Module.builder().spec("module:name").version("0.0.1-SNAPSHOT").build()),
                is(graph.depending(Module.builder().spec("module:name").version("0.0.1-SNAPSHOT").build())));
    }
}
