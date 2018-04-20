package org.codingmatters.poom.ci.pipeline;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.pipeline.descriptors.json.PipelineReader;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PipelineReaderTest {
    @Test
    public void fromYaml() throws Exception {
        PipelineReader reader = new PipelineReader();
        YAMLFactory factory = new YAMLFactory();
        try(JsonParser parser = factory.createParser(Thread.currentThread().getContextClassLoader().getResourceAsStream("poom-ci-pipeline.yaml"))) {
            Pipeline pipeline = reader.read(parser);
            assertThat(
                    pipeline,
                    is(Pipeline.builder()
                            .stages( stage -> stage
                                    .name("build")
                                    .exec("$MVN clean install -DskipTests -Ddocker.resource.docker.url=http://172.17.0.1:2375")
                            )
                            .env(ObjectValue.builder()
                                    .property("MVN", v -> v
                                            .stringValue("docker run -it --rm -v $SRC:/src -v $WORKSPACE/M2:/root/.m2 flexio-build-java mvn")
                                    )
                                    .build())
                            .build())
            );
        }
    }
}
