package org.codingmatters.poom.ci.runners.pipeline;

@FunctionalInterface
public interface PipelineExecutorProvider {
    PipelineExecutor forContext(PipelineContext context);
}
