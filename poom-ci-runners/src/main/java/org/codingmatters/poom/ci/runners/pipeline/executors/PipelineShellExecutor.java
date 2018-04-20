package org.codingmatters.poom.ci.runners.pipeline.executors;

import org.codingmatters.poom.ci.pipeline.api.types.StageTermination;
import org.codingmatters.poom.ci.runners.pipeline.PipelineContext;
import org.codingmatters.poom.ci.runners.pipeline.PipelineExecutor;

import java.io.IOException;

public class PipelineShellExecutor implements PipelineExecutor {

    private final PipelineContext context;

    public PipelineShellExecutor(PipelineContext context) {
        this.context = context;
    }

    @Override
    public void initialize() throws IOException {

    }

    @Override
    public StageTermination.Exit execute(String stage, StageLogListener logListener) throws IOException {
        this.context.pipeline().stages().stream()
                .filter(stg -> stg.name().equals(stage)).findFirst()
                .orElseThrow(() -> new IOException("no stage " + stage));


        return null;
    }
}
