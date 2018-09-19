package org.codingmatters.poom.ci.runners.pipeline.providers.gh;

import org.codingmatters.poom.ci.runners.pipeline.PipelineVariables;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.process.ProcessInvoker;

import java.io.File;
import java.io.IOException;

public class FetchCheckoutStrategy implements CheckoutStrategy {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(FetchCheckoutStrategy.class);

    @Override
    public void checkout(PipelineVariables vars, File workspace) throws AbstractGitHubPipelineContextProvider.ProcessingException {
        ProcessInvoker invoker = new ProcessInvoker();
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(workspace)
                ;

        try {
            int status;
            log.info("initializing local repo");
            status = invoker.exec(processBuilder.command("git", "init"), line -> log.info(line), line -> log.error(line));
            if(status != 0) throw new AbstractGitHubPipelineContextProvider.ProcessingException("git init exited with a none 0 status");

            log.info("fetching remote repo : {}, {}", vars.repositoryUrl(), vars.branch());
            status = invoker.exec(processBuilder.command("git", "fetch", "-u", vars.repositoryUrl(), vars.branch()), line -> log.info(line), line -> log.error(line));
            if(status != 0) throw new AbstractGitHubPipelineContextProvider.ProcessingException("git fetch exited with a none 0 status");

            String checkoutTarget = "FETCH_HEAD";
            if(vars.changeset() != null && ! vars.changeset().isEmpty()) {
                checkoutTarget = vars.changeset();
            }
            log.info("checking out {}", checkoutTarget);
            status = invoker.exec(processBuilder.command("git", "checkout", checkoutTarget), line -> log.info(line), line -> log.error(line));
            if (status != 0) throw new AbstractGitHubPipelineContextProvider.ProcessingException("git checkout exited with a none 0 status");
        } catch (InterruptedException | IOException e) {
            throw new AbstractGitHubPipelineContextProvider.ProcessingException("exception raised whlie checking out workspace", e);
        }
    }
}
