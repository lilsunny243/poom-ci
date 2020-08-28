package org.codingmatters.poom.ci.apps.releaser;

import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.command.exception.CommandFailed;
import org.codingmatters.poom.ci.apps.releaser.flow.FlexioFlow;
import org.codingmatters.poom.ci.apps.releaser.git.Git;
import org.codingmatters.poom.ci.apps.releaser.git.GitRepository;

import java.io.File;
import java.util.UUID;

public class Release {
    private final String repositoryUrl;
    private final CommandHelper commandHelper;

    public Release(String repositoryUrl, CommandHelper commandHelper) {
        this.repositoryUrl = repositoryUrl;
        this.commandHelper = commandHelper;
    }

    public String initiate() throws CommandFailed {
        File workspace = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID().toString());
        workspace.mkdir();

        GitRepository repository = new Git(workspace, this.commandHelper).clone(this.repositoryUrl);
        FlexioFlow flow = new FlexioFlow(workspace, this.commandHelper);
        repository.checkout("develop");

        System.out.println("releasing " + flow.version());

        flow.startRelease();
        String releaseVersion = flow.version();
        repository.merge("master", "release auto merge");
        flow.finishRelease();

        return releaseVersion;
    }
}
