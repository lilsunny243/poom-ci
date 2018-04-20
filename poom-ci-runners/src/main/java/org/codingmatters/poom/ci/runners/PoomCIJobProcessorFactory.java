package org.codingmatters.poom.ci.runners;

import org.codingmatters.poom.ci.runners.pipeline.PipelineJobProcessor;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poomjobs.api.types.Job;

public class PoomCIJobProcessorFactory implements JobProcessor.Factory {

    @Override
    public JobProcessor createFor(Job job) {
        if(job.name().equals("pipeline")) {
            return new PipelineJobProcessor(job);
        }
        return null;
    }

}
