package org.codingmatters.poom.ci.runners.pipeline.providers.gh;

import org.codingmatters.poom.ci.runners.git.CloneRepository;
import org.codingmatters.poom.ci.runners.pipeline.PipelineVariables;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.io.File;

public class CloneCheckoutStrategy implements CheckoutStrategy {
    static private CategorizedLogger log = CategorizedLogger.getLogger(CloneCheckoutStrategy.class);

    @Override
    public void checkout(PipelineVariables vars, File workspace) throws AbstractGitHubPipelineContextProvider.ProcessingException {
        String repositoryUrl = vars.repositoryUrl();
        String branch = vars.branch();
        String changeset = vars.changeset();

        new CloneRepository(repositoryUrl, branch, changeset).to(workspace);
    }
}
