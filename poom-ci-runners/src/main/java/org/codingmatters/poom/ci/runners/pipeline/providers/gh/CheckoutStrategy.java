package org.codingmatters.poom.ci.runners.pipeline.providers.gh;

import org.codingmatters.poom.ci.runners.pipeline.PipelineVariables;

import java.io.File;

public interface CheckoutStrategy {
    static CheckoutStrategy strategy() {
        return new CloneCheckoutStrategy();
    }

    void checkout(PipelineVariables vars, File workspace) throws AbstractGitHubPipelineContextProvider.ProcessingException;
}
