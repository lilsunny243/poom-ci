package org.codingmatters.poom.ci.apps.releaser.git;

import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.command.exception.CommandFailed;

import java.io.File;

public class Git {
    private final File workspace;
    private final CommandHelper commandHelper;

    public Git(File workspace, CommandHelper commandHelper) {
        this.workspace = workspace;
        this.commandHelper = commandHelper;
    }

    public GitRepository clone(String repositoryUrl) throws CommandFailed {
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(workspace)
                .command("git", "clone", repositoryUrl, workspace.getAbsolutePath())
                ;
        this.commandHelper.exec(processBuilder, "git clone " + repositoryUrl);
        return new GitRepository(this.commandHelper, this.workspace);
    }

    public String checkoutSpec() {
        //git config --get remote.origin.url
        return null;
    }
}
