package org.codingmatters.poom.ci.pipeline;

import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.pipeline.descriptors.Secret;
import org.codingmatters.poom.ci.pipeline.descriptors.ValueList;
import org.codingmatters.poom.ci.pipeline.merge.SimplePipelineMerger;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PipelineMergerTest {

    @Test
    public void givenEmptyPipeline__whenMergingWithEmptyPipeline__thenMergedIsEmpty() throws Exception {
        assertThat(
                new SimplePipelineMerger().merge(
                        Pipeline.builder().build(),
                        Pipeline.builder().build()
                ),
                is(Pipeline.builder().build())
        );
    }

    @Test
    public void givenEmptyPipeline__whenMergingWithPipelineWithStages__thenMergesHaveThoseStages() throws Exception {
        assertThat(
                new SimplePipelineMerger().merge(
                        Pipeline.builder()
                                .stages(stage -> stage.name("S"))
                                .cleanup(stage -> stage.name("S"))
                                .onError(stage -> stage.name("S"))
                                .onSuccess(stage -> stage.name("S"))
                                .build(),
                        Pipeline.builder().build()
                        ),
                is(Pipeline.builder()
                        .stages(stage -> stage.name("S"))
                        .cleanup(stage -> stage.name("S"))
                        .onError(stage -> stage.name("S"))
                        .onSuccess(stage -> stage.name("S"))
                        .build())
        );
    }

    @Test
    public void givenPipelineWithStages__whenMergingWithEmptyPipeline__thenPipelineHaveTheOriginalStages() throws Exception {
        assertThat(
                new SimplePipelineMerger().merge(
                        Pipeline.builder().build(),
                        Pipeline.builder()
                                .stages(stage -> stage.name("S"))
                                .cleanup(stage -> stage.name("S"))
                                .onError(stage -> stage.name("S"))
                                .onSuccess(stage -> stage.name("S"))
                                .build()
                ),
                is(Pipeline.builder()
                        .stages(stage -> stage.name("S"))
                        .cleanup(stage -> stage.name("S"))
                        .onError(stage -> stage.name("S"))
                        .onSuccess(stage -> stage.name("S"))
                        .build())
        );
    }

    @Test
    public void givenPipelineWithStages__whenInsertingBeforeAnExistingStage__thenPipelineHasTwoStages_andTheOriginalIsAfterTheNewOne() throws Exception {
        assertThat(
                new SimplePipelineMerger().merge(
                        Pipeline.builder()
                                .stages(stage -> stage.name("S2").before("S1"))
                                .build(),
                        Pipeline.builder()
                                .stages(stage -> stage.name("S1"))
                                .build()
                ),
                is(Pipeline.builder()
                        .stages(stage -> stage.name("S2"), stage -> stage.name("S1"))
                        .build())
        );
    }

    @Test
    public void givenPipelineWithStages__whenInsertingBeforeAnUnExistingStage__thenPipelineHasTwoStages_andTheNewOneIsAppended() throws Exception {
        assertThat(
                new SimplePipelineMerger().merge(
                        Pipeline.builder()
                                .stages(stage -> stage.name("S2").before("S0"))
                                .build(),
                        Pipeline.builder()
                                .stages(stage -> stage.name("S1"))
                                .build()
                ),
                is(Pipeline.builder()
                        .stages(stage -> stage.name("S1"), stage -> stage.name("S2"))
                        .build())
        );
    }

    @Test
    public void givenPipelineWithStages__whenInsertingAfterAnExistingStage__thenPipelineHasTwoStages_andTheOriginalIsAfterTheNewOne() throws Exception {
        assertThat(
                new SimplePipelineMerger().merge(
                        Pipeline.builder()
                                .stages(stage -> stage.name("S2").after("S1"))
                                .build(),
                        Pipeline.builder()
                                .stages(stage -> stage.name("S1"), stage -> stage.name("S3"))
                                .build()
                ),
                is(Pipeline.builder()
                        .stages(stage -> stage.name("S1"), stage -> stage.name("S2"), stage -> stage.name("S3"))
                        .build())
        );
    }

    @Test
    public void givenPipelineWithStages__whenInsertingAfterAnUnExistingStage__thenPipelineHasTwoStages_andTheNewOneIsAppended() throws Exception {
        assertThat(
                new SimplePipelineMerger().merge(
                        Pipeline.builder()
                                .stages(stage -> stage.name("S2").after("S0"))
                                .build(),
                        Pipeline.builder()
                                .stages(stage -> stage.name("S1"), stage -> stage.name("S3"))
                                .build()
                ),
                is(Pipeline.builder()
                        .stages(stage -> stage.name("S1"), stage -> stage.name("S3"), stage -> stage.name("S2"))
                        .build())
        );
    }

    @Test
    public void givenEmptyPipeline__whenMergingWithAPipelineWithEnv__thenEnvIsAppendend() throws Exception {
        assertThat(
                new SimplePipelineMerger().merge(
                        Pipeline.builder()
                                .env(ObjectValue.builder()
                                        .property("e", v -> v.stringValue("v"))
                                        .build())
                                .build(),
                        Pipeline.builder().build()
                ),
                is(Pipeline.builder()
                        .env(ObjectValue.builder()
                                .property("e", v -> v.stringValue("v"))
                                .build())
                        .build())
        );
    }

    @Test
    public void givenNoEmptyPipeline__whenMergingWithAPipelineWithEnv__thenEnvIsAppendend() throws Exception {
        assertThat(
                new SimplePipelineMerger().merge(
                        Pipeline.builder()
                                .env(ObjectValue.builder()
                                        .property("e", v -> v.stringValue("v"))
                                        .build())
                                .build(),
                        Pipeline.builder()
                                .env(ObjectValue.builder()
                                        .property("o", v -> v.stringValue("v"))
                                        .build())
                                .build()
                ),
                is(Pipeline.builder()
                        .env(
                                ObjectValue.builder().property("o", v -> v.stringValue("v")).build(),
                                ObjectValue.builder().property("e", v -> v.stringValue("v")).build()
                        )
                        .build())
        );
    }

    @Test
    public void givenNoEmptyPipeline__whenMergingWithAPipelineWithExistingEnv__thenEnvIsAppendendButNotMerged() throws Exception {
        assertThat(
                new SimplePipelineMerger().merge(
                        Pipeline.builder()
                                .env(ObjectValue.builder()
                                        .property("e", v -> v.stringValue("v2"))
                                        .build())
                                .build(),
                        Pipeline.builder()
                                .env(ObjectValue.builder()
                                        .property("e", v -> v.stringValue("v1"))
                                        .build())
                                .build()
                ),
                is(Pipeline.builder()
                        .env(
                                ObjectValue.builder().property("e", v -> v.stringValue("v1")).build(),
                                ObjectValue.builder().property("e", v -> v.stringValue("v2")).build()
                        )
                        .build())
        );
    }

    @Test
    public void givenPipelineWithNoSecret__whenMergingWithPipelineWithNoSecret__thenMergedHasNoSecrets() throws Exception {
        assertThat(
                new SimplePipelineMerger().merge(
                        Pipeline.builder().build(),
                        Pipeline.builder().build()
                ).secrets(),
                is(nullValue())
        );
    }

    @Test
    public void givenPipelineWithNoSecret__whenMergingWithPipelineHavingSecrets__thenMergedHasThoseSecrets() throws Exception {
        assertThat(
                new SimplePipelineMerger().merge(
                        Pipeline.builder()
                                .secrets(
                                        Secret.builder().name("new secret 1").build(),
                                        Secret.builder().name("new secret 2").build()
                                )
                                .build(),
                        Pipeline.builder()
                                .build()
                ).secrets(),
                is(new ValueList.Builder<Secret>().with(
                        Secret.builder().name("new secret 1").build(),
                        Secret.builder().name("new secret 2").build()
                ).build())
        );
    }

    @Test
    public void givenPipelineWithSomeSecret__whenMergingWithPipelineWithNoSecret__thenMergedHasThoseSecrets() throws Exception {
        assertThat(
                new SimplePipelineMerger().merge(
                        Pipeline.builder()
                                .build(),
                        Pipeline.builder()
                                .secrets(
                                        Secret.builder().name("secret 1").build(),
                                        Secret.builder().name("secret 2").build()
                                )
                                .build()
                ).secrets(),
                is(new ValueList.Builder<Secret>().with(
                        Secret.builder().name("secret 1").build(),
                        Secret.builder().name("secret 2").build()
                ).build())
        );
    }

    @Test
    public void givenPipelineWithSomeSecret__whenMergingWithPipelineHavingSecrets__thenMergedHasAllSecrets() throws Exception {
        assertThat(
                new SimplePipelineMerger().merge(
                        Pipeline.builder()
                                .secrets(
                                        Secret.builder().name("new secret 1").build(),
                                        Secret.builder().name("new secret 2").build()
                                )
                                .build(),
                        Pipeline.builder()
                                .secrets(
                                        Secret.builder().name("secret 1").build(),
                                        Secret.builder().name("secret 2").build()
                                )
                                .build()
                ).secrets(),
                is(new ValueList.Builder<Secret>().with(
                        Secret.builder().name("secret 1").build(),
                        Secret.builder().name("secret 2").build(),
                        Secret.builder().name("new secret 1").build(),
                        Secret.builder().name("new secret 2").build()
                ).build())
        );
    }

    @Test
    public void givenPipelineWithSomeSecret__whenMergingWithPipelineHavingSameSecretName__thenMergedOneOverridenSecret() throws Exception {
        assertThat(
                new SimplePipelineMerger().merge(
                        Pipeline.builder()
                                .secrets(
                                        Secret.builder().name("secret").content("new content").build()
                                )
                                .build(),
                        Pipeline.builder()
                                .secrets(
                                        Secret.builder().name("secret").content("original content").build()
                                )
                                .build()
                ).secrets(),
                is(new ValueList.Builder<Secret>().with(
                        Secret.builder().name("secret").content("new content").build()
                ).build())
        );
    }
}