package org.codingmatters.poom.ci.runners.pipeline;

import org.codingmatters.poom.ci.pipeline.api.PoomCIPipelineAPIHandlers;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIHandlersClient;
import org.codingmatters.poomjobs.api.types.Job;
import org.junit.Test;

import java.util.concurrent.Executors;

public class PipelineJobProcessorTest {

    private PoomCIPipelineAPIClient pipelineClient = new PoomCIPipelineAPIHandlersClient(
            new PoomCIPipelineAPIHandlers.Builder().build(),
            Executors.newFixedThreadPool(4)
    );

    @Test
    public void name() throws Exception {
        Job job = Job.builder()
                .category("poom-ci").name("pipeline").arguments("pipeline-id")
                .build();

        job = new PipelineJobProcessor(job).process();
    }
}