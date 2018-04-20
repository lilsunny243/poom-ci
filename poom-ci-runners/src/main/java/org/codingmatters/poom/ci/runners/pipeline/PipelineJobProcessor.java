package org.codingmatters.poom.ci.runners.pipeline;

import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poomjobs.api.types.Job;

public class PipelineJobProcessor implements JobProcessor {
    private final Job job;

    public PipelineJobProcessor(Job job) {
        this.job = job;
    }

    @Override
    public Job process() throws JobProcessingException {
        return null;
    }
}
