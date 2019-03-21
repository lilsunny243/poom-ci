package org.codingmatters.poom.ci.runners.git;

import org.codingmatters.poom.ci.runners.pipeline.providers.gh.AbstractGitHubPipelineContextProvider;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.process.ProcessInvoker;

import java.io.File;
import java.io.IOException;

public class CloneRepository {

    static private CategorizedLogger log = CategorizedLogger.getLogger(CloneRepository.class);
    private final String repositoryUrl;
    private final String branch;
    private final String changeset;

    public CloneRepository(String repositoryUrl, String branch, String changeset) {
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.changeset = changeset;
    }

    public String repositoryUrl() {
        return repositoryUrl;
    }

    public String branch() {
        return branch;
    }

    public String changeset() {
        return changeset;
    }

    public void to(File workspace) throws AbstractGitHubPipelineContextProvider.ProcessingException {
        ProcessInvoker invoker = new ProcessInvoker();
        ProcessBuilder processBuilder = new ProcessBuilder().directory(workspace);

        try {
            int status;

            log.info("cloning remote repo : {}, {}", repositoryUrl, branch);
            status = invoker.exec(processBuilder.command("git", "clone", repositoryUrl, workspace.getAbsolutePath()), line -> log.info(line), line -> log.error(line));
            if (status != 0)
                throw new AbstractGitHubPipelineContextProvider.ProcessingException("git clone exited with a none 0 status");

            String checkoutTarget = branch;
            if (changeset != null && !changeset.isEmpty()) {
                checkoutTarget = changeset;
            }
            log.info("checking out {}", checkoutTarget);
            status = invoker.exec(processBuilder.command("git", "checkout", checkoutTarget), line -> log.info(line), line -> log.error(line));
            if (status != 0)
                throw new AbstractGitHubPipelineContextProvider.ProcessingException("git checkout " + checkoutTarget + " exited with a none 0 status");
        } catch (InterruptedException | IOException e) {
            throw new AbstractGitHubPipelineContextProvider.ProcessingException("exception raised whlie checking out workspace", e);
        }
    }
}