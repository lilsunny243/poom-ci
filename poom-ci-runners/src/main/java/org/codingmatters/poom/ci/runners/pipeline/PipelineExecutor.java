package org.codingmatters.poom.ci.runners.pipeline;

import org.codingmatters.poom.ci.pipeline.api.types.StageTermination;
import org.codingmatters.poom.ci.pipeline.descriptors.StageHolder;

import java.io.IOException;

public interface PipelineExecutor {
    @FunctionalInterface
    interface PipelineExecutorProvider {
        PipelineExecutor forContext(PipelineContext context);
    }

    void initialize() throws IOException;
    StageTermination.Exit execute(StageHolder stage, StageLogListener logListener) throws IOException;

    @FunctionalInterface
    interface StageLogListener {
        void logLine(String log);
    }
}
