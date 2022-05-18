package org.codingmatters.poom.ci.apps.releaser.git;

import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.command.exception.CommandFailed;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

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

    public String branch() throws CommandFailed {
        String[] lines = this.commandHelper.execWithStdout(new ProcessBuilder("git", "branch").directory(workspace), "git branch");
        for (String line : lines) {
            if(line.startsWith("*")) {
                return line.replace("*", "").trim();
            }
        }
        return null;
    }

    public String remoteOrigin() throws CommandFailed {
        String[] lines = this.commandHelper.execWithStdout(
                new ProcessBuilder("git", "config", "--get", "remote.origin.url").directory(workspace),
                "git config --get remote.origin.url"
        );
        return lines.length> 0 ? lines[0] : null;
    }

    public String username() throws CommandFailed {
        String[] lines = this.commandHelper.execWithStdout(
                new ProcessBuilder("git", "config", "user.name").directory(workspace),
                "git config user.name"
        );
        return lines.length> 0 ? lines[0] : null;
    }

    public String email() throws CommandFailed {
        String[] lines = this.commandHelper.execWithStdout(
                new ProcessBuilder("git", "config", "user.email").directory(workspace),
                "git config user.name"
        );
        return lines.length> 0 ? lines[0] : null;
    }


    public String checkoutSpec() throws CommandFailed {
        return String.format("git|%s|%s", this.remoteOrigin(), this.branch());
    }
}
