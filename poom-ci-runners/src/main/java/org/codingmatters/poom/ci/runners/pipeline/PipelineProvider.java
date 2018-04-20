package org.codingmatters.poom.ci.runners.pipeline;

import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;

import java.io.IOException;

public interface PipelineProvider {
    Pipeline pipeline() throws IOException;
}
