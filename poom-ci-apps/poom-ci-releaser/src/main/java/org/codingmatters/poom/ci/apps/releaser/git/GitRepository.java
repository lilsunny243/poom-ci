package org.codingmatters.poom.ci.apps.releaser.git;

import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.command.exception.CommandFailed;

import java.io.File;

public class GitRepository {
    private final CommandHelper commandHelper;
    private final File repository;

    public GitRepository(CommandHelper commandHelper, File repository) {
        this.commandHelper = commandHelper;
        this.repository = repository;
    }

    public void checkout(String branch) throws CommandFailed {
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(this.repository)
                .command("git", "checkout", branch)
                ;
        this.commandHelper.exec(processBuilder, "git checkout " + branch);
    }

    public void merge(String withBranch, String message) throws CommandFailed {
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(this.repository)
                .command("git", "merge", withBranch, "-m", message)
                ;
        this.commandHelper.exec(processBuilder, "git merge " + withBranch);
    }

    public void commit(String message) throws CommandFailed {
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(this.repository)
                .command("git", "commit", "-am", message)
                ;
        this.commandHelper.exec(processBuilder, "git commit");
    }
}
