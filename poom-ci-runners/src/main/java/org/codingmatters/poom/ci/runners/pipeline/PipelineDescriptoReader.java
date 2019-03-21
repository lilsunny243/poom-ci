package org.codingmatters.poom.ci.runners.pipeline;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.pipeline.descriptors.json.PipelineReader;

import java.io.File;
import java.io.IOException;

public class PipelineDescriptoReader {
    private final YAMLFactory yamlFactory;

    public PipelineDescriptoReader(YAMLFactory yamlFactory) {
        this.yamlFactory = yamlFactory;
    }

    public Pipeline read(File workspace) throws NotAPipelineContextException, IOException {
        File pipelineDescriptor = new File(workspace, "poom-ci-pipeline.yaml");
        if(! pipelineDescriptor.exists()) {
            throw new NotAPipelineContextException(pipelineDescriptor.getAbsolutePath());
        }
        try(JsonParser parser = this.yamlFactory.createParser(pipelineDescriptor)) {
            return new PipelineReader().read(parser);
        }
    }
}
