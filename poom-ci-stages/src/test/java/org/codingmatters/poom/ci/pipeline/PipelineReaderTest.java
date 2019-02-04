package org.codingmatters.poom.ci.pipeline;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.pipeline.descriptors.json.PipelineReader;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;

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
                            .stages(
                                stage -> stage
                                    .name("build")
                                    .exec("$MVN clean install -DskipTests -Ddocker.resource.docker.url=http://172.17.0.1:2375"),
                                stage -> stage
                                    .name("only when on master")
                                    .onlyWhen("branch in (master)"),
                                stage -> stage
                                    .name("metadata")
                                    .exec(
                                            "$NOTIFY --data-urlencode \"status=2\" --data-urlencode \"current-stage=$STAGE\"",
                                            "$UPLOAD_PROJECT_METADATA")
                                    .onlyWhen("branch in (master, develop)")
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


    public static void main(String[] args) {
        if(args.length < 1) throw new RuntimeException("need an argument to read pipeline from");

        PipelineReader reader = new PipelineReader();
        YAMLFactory factory = new YAMLFactory();
        try(JsonParser parser = factory.createParser(new FileInputStream(args[0]))) {
            System.out.println(reader.read(parser));
        } catch (IOException e) {
            throw new RuntimeException("failed reading pipeline : " + args[0], e);
        }
    }
}
