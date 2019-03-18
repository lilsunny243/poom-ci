package org.codingmatters.poom.ci.pipeline;

import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;

@FunctionalInterface
public interface PipelineMerger {

    Pipeline merge(Pipeline pipeline,Pipeline into);

}
