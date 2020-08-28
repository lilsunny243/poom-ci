package org.codingmatters.poom.ci.apps.releaser.git;

import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.command.exception.CommandFailed;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.io.FileMatchers.anExistingFile;

public class GitTest {

    static private final String REPO_URL = "git@github.com:flexiooss/poom-ci-releaser-tests.git";

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    @Test
    public void whenCloning__thenWorkspaceIsFilledWithRepoContent() throws Exception {
        File workspace = this.dir.newFolder();
        Git git = new Git(workspace, new CommandHelper(line -> System.out.println(line), line -> System.err.println(line)));

        GitRepository repo = git.clone(REPO_URL);

        assertThat(new File(workspace, "README.md"), is(anExistingFile()));
    }

    @Test
    public void givenClonedRepo__whenCheckingoutDevelop__thenDevelopIsCheckeout() throws Exception {
        File workspace = this.dir.newFolder();
        Git git = new Git(workspace, new CommandHelper(line -> System.out.println(line), line -> System.err.println(line)));
        GitRepository repo = git.clone(REPO_URL);

        repo.checkout("develop");

        assertThat(this.currentBranch(workspace), is("develop"));
//        assertThat(this.content(new File(workspace, "README.md")), is("DEVELOP\n"));
    }

    @Test
    public void givenClonedRepo__whenCheckingoutMaster__thenMasterIsCheckeout() throws Exception {
        File workspace = this.dir.newFolder();
        Git git = new Git(workspace, new CommandHelper(line -> System.out.println(line), line -> System.err.println(line)));
        GitRepository repo = git.clone(REPO_URL);

        repo.checkout("master");

        assertThat(this.currentBranch(workspace), is("master"));

//        assertThat(this.content(new File(workspace, "README.md")), is("MASTER\n"));
    }

    private String content(File f) throws IOException {
        try(FileReader reader = new FileReader(f)) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[1024];
            for(int read = reader.read(buffer) ; read != -1 ; read = reader.read(buffer)) {
                result.append(buffer, 0, read);
            }
            return result.toString();
        }
    }

    private void printTree(File parent, String prefix) {
        if(parent.isDirectory()) {
            System.out.printf("%s+ %s\n", prefix, parent.getName());
            for (File child : parent.listFiles()) {
                this.printTree(child, prefix + "  ");
            }
        } else {
            System.out.printf("%s- %s\n", prefix, parent.getName());
        }
    }

    private String currentBranch(File workspace) throws CommandFailed {
        AtomicReference<String> branch = new AtomicReference<>();
        new CommandHelper(line -> {
            if(line.startsWith("*")) {
                branch.set(line);
            }
        }, line -> System.err.println(line)).exec(new ProcessBuilder("git", "branch").directory(workspace), "");

        if(branch.get() != null) {
            return branch.get().replace("*", "").trim();
        } else {
            return null;
        }

    }
}