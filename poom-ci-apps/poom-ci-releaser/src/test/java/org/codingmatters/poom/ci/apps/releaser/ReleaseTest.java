package org.codingmatters.poom.ci.apps.releaser;

import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.graph.PropagationContext;
import org.codingmatters.poom.ci.apps.releaser.maven.pom.ArtifactCoordinates;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

@Ignore
public class ReleaseTest {

    static private final String REPO_URL = "git@github.com:flexiooss/poom-ci-releaser-tests.git";

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    @Test
    public void givenExistingRepo__whenReleasing__thenReleased() throws Exception {
        Release release = new Release(
                REPO_URL,
                new PropagationContext(),
                new CommandHelper(line -> System.out.println(line), line -> System.err.println(line)),
                new Workspace(this.dir.getRoot())
        );
        ArtifactCoordinates version = release.initiate();
        System.out.println(version + " released");
    }
}