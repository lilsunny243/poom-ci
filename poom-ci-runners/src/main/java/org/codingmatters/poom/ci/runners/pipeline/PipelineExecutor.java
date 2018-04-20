package org.codingmatters.poom.ci.runners.pipeline;

import org.codingmatters.poom.ci.pipeline.api.types.StageTermination;

import java.io.IOException;

public interface PipelineExecutor {
    void initialize() throws IOException;
    StageTermination.Exit execute(String stage, StageLogListener logListener) throws IOException;

    @FunctionalInterface
    interface StageLogListener {
        void logLine(String log);
    }
}
