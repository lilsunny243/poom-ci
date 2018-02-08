package org.codingmatters.poom.ci.pipeline;

import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PipelineScriptTest {

    @Test
    public void stageScript() throws Exception {
        Pipeline pipeline = Pipeline.builder()
                .stages(stage -> stage
                        .name("build")
                        .exec("$MVN clean install -DskipTests -Ddocker.resource.docker.url=http://172.17.0.1:2375")
                )
                .env(ObjectValue.builder()
                        .property("MVN", v -> v
                                .stringValue("docker run -it --rm -v $SRC:/src -v $WORKSPACE/M2:/root/.m2 flexio-build-java mvn")
                        )
                        .build())
                .build();

        PipelineScript script = new PipelineScript(pipeline);

        try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            script.forStage("build", out);
            out.flush();
            out.close();
            assertThat(out.toString(), is(this.resourceAsString("poom-ci-build-stage.sh")));
        }
    }

    @Test
    public void pipelineScript() throws Exception {
        Pipeline pipeline = Pipeline.builder()
                .stages(stage -> stage.name("first").exec("A", "B"),
                        stage -> stage.name("second").exec("C")
                )
                .env(ObjectValue.builder()
                        /*
                        X="x-value"
                        Y="y-value"
                        Z="z-value"
                         */
                        .property("X", v -> v.stringValue("x-value"))
                        .property("Y", v -> v.stringValue("y-value"))
                        .property("Z", v -> v.stringValue("z-value"))
                        .build())
                .build();

        PipelineScript script = new PipelineScript(pipeline);

        try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            script.forPipeline(out);
            out.flush();
            out.close();
            assertThat(out.toString(), is(this.resourceAsString("poom-ci-build-pipeline.sh")));
        }
    }

    private String resourceAsString(String resource) throws IOException {
        try(
                InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource) ;
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte [] buffer = new byte[1024];
            for(int read = in.read(buffer) ; read != -1 ; read = in.read(buffer)) {
                out.write(buffer, 0, read);
            }

            out.flush();
            out.close();
            return out.toString();
        }
    }
}