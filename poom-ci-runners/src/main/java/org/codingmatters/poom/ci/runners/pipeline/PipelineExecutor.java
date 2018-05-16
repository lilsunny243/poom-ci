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
    boolean isExecutable(StageHolder stage) throws InvalidStageRestrictionException;
    StageTermination.Exit execute(StageHolder stage, StageLogListener logListener) throws IOException;

    @FunctionalInterface
    interface StageLogListener {
        void logLine(String log);
    }

    class InvalidStageRestrictionException extends Exception {
        public InvalidStageRestrictionException(String s) {
            super(s);
        }

        public InvalidStageRestrictionException(String s, Throwable throwable) {
            super(s, throwable);
        }
    }
}
