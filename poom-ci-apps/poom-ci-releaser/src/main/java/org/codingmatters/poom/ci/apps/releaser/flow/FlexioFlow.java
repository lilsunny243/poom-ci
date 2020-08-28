package org.codingmatters.poom.ci.apps.releaser.flow;

import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.command.exception.CommandFailed;

import java.io.File;

public class FlexioFlow {
    private final File repository;
    private final CommandHelper commandHelper;

    public FlexioFlow(File repository, CommandHelper commandHelper) {
        this.repository = repository;
        this.commandHelper = commandHelper;
    }

    public String version() throws CommandFailed {
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(this.repository)
                .command("flexio-flow", "version")
                ;

        String[] results = this.commandHelper.execWithStdout(processBuilder, "flexio-flow version");
        return results[0];
    }

    public void startRelease() throws CommandFailed {
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(this.repository)
                .command("flexio-flow", "release", "start", "-D")
                ;
        this.commandHelper.exec(processBuilder, "flexio-flow release start -D");
    }

    public void finishRelease() throws CommandFailed {
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(this.repository)
                .command("flexio-flow", "release", "finish", "-D")
                ;
        this.commandHelper.exec(processBuilder, "flexio-flow release finish -D");
    }
}
