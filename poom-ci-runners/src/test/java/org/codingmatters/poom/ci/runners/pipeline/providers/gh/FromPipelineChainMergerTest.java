package org.codingmatters.poom.ci.runners.pipeline.providers.gh;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.pipeline.descriptors.RepositorySpec;
import org.codingmatters.poom.ci.pipeline.descriptors.Secret;
import org.codingmatters.poom.ci.pipeline.descriptors.Stage;
import org.codingmatters.poom.ci.runners.git.CloneRepository;
import org.codingmatters.poom.ci.runners.pipeline.PipelineDescriptoReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.function.Function;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FromPipelineChainMergerTest {

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    static private final Function<RepositorySpec, CloneRepository> GET_CLONER = spec -> new TestCloneRepository(spec.url(), spec.branch(), null);
    private PipelineDescriptoReader pipelineDescriptoReader = new PipelineDescriptoReader(new YAMLFactory());

    @Test
    public void testCloneRepository() throws Exception {
        new TestCloneRepository("repo1", "master", null).to(this.dir.getRoot());

        assertTrue(new File(this.dir.getRoot(), "poom-ci-pipeline.yaml").exists());
        assertTrue(new File(this.dir.getRoot(), ".secrets/secret.file").exists());
    }

    @Test
    public void givenPipelineHasFromRepoWithSecrets__whenMergingPipeline__thenSecretsAreMerged() throws Exception {
        FromPipelineChainMerger merger = new FromPipelineChainMerger(
                Pipeline.builder()
                        .from(RepositorySpec.builder().url("repo1").branch("master").build())
                        .stages(Stage.builder().name("stage0").build())
                        .build(),
                this.pipelineDescriptoReader,
                GET_CLONER);

        Pipeline actual = merger.mergedPipeline();

        assertThat(
                actual,
                is(Pipeline.builder()
                        .stages(
                                Stage.builder().name("st1").build(),
                                Stage.builder().name("stage0").build()
                        )
                        .secrets(
                                Secret.builder()
                                        .name("secr")
                                        .content("$SRC/.secrets/secret.file")
                                        .as(Secret.As.file)
                                        .build()
                        )
                        .build())
        );
    }

    @Test
    public void givenPipelineHasFromRepoWithSecrets__whenMergingSources__thenFilesAreCopied() throws Exception {
        FromPipelineChainMerger merger = new FromPipelineChainMerger(
                Pipeline.builder()
                        .from(RepositorySpec.builder().url("repo1").branch("master").build())
                        .stages(Stage.builder().name("stage0").build())
                        .build(),
                this.pipelineDescriptoReader,
                GET_CLONER);

        merger.mergeSources(this.dir.getRoot());

        assertTrue(new File(this.dir.getRoot(), ".secrets/secret.file").exists());
    }

    @Test
    public void givenPipelineHasFromRepo__whenMergingSources__thenPipelineFileIsNotOverriden() throws Exception {
        FromPipelineChainMerger merger = new FromPipelineChainMerger(
                Pipeline.builder()
                        .from(RepositorySpec.builder().url("repo1").branch("master").build())
                        .stages(Stage.builder().name("stage0").build())
                        .build(),
                this.pipelineDescriptoReader,
                GET_CLONER);

        this.storeFile(new File(this.dir.getRoot(), "poom-ci-pipeline.yaml"), "not overridden");

        merger.mergeSources(this.dir.getRoot());

        assertThat(this.readeFile(new File(this.dir.getRoot(), "poom-ci-pipeline.yaml")), is("not overridden"));
    }

    @Test
    public void givenPipelineHasTwoFromRepo__whenMerging__thenPipelinesAreMerged() throws Exception {
        FromPipelineChainMerger merger = new FromPipelineChainMerger(
                Pipeline.builder()
                        .from(
                                RepositorySpec.builder().url("repo2").branch("master").build(),
                                RepositorySpec.builder().url("repo4").branch("master").build()
                        )
                        .stages(Stage.builder().name("stage0").build())
                        .build(),
                this.pipelineDescriptoReader,
                GET_CLONER);

        Pipeline actual = merger.mergedPipeline();

        assertThat(
                actual,
                is(Pipeline.builder()
                        .stages(
                                Stage.builder().name("st4").build(),
                                Stage.builder().name("st2").build(),
                                Stage.builder().name("stage0").build()
                        )
                        .build())
        );
    }

    @Test
    public void givenPipelineHasFromRepoWithFrom__whenMergingPipeline__thenPipelinesAreRecursivelyMerged() throws Exception {
        FromPipelineChainMerger merger = new FromPipelineChainMerger(
                Pipeline.builder()
                        .from(
                                RepositorySpec.builder().url("repo3").branch("master").build()
                        )
                        .stages(Stage.builder().name("stage0").build())
                        .build(),
                this.pipelineDescriptoReader,
                GET_CLONER);

        Pipeline actual = merger.mergedPipeline();

        assertThat(
                actual,
                is(Pipeline.builder()
                        .stages(
                                Stage.builder().name("st3").build(),
                                Stage.builder().name("st1").build(),
                                Stage.builder().name("stage0").build()
                        )
                        .secrets(
                                Secret.builder()
                                        .name("secr")
                                        .content("$SRC/.secrets/secret.file")
                                        .as(Secret.As.file)
                                        .build()
                        )
                        .build())
        );
    }

    @Test
    public void givenPipelineHasFromRepoWithFrom__whenMergingSources__thenFilesAreCopied() throws Exception {
        FromPipelineChainMerger merger = new FromPipelineChainMerger(
                Pipeline.builder()
                        .from(RepositorySpec.builder().url("repo3").branch("master").build())
                        .stages(Stage.builder().name("stage0").build())
                        .build(),
                this.pipelineDescriptoReader,
                GET_CLONER);

        merger.mergeSources(this.dir.getRoot());

        assertTrue(new File(this.dir.getRoot(), ".secrets/secret.file").exists());
    }


    private void storeFile(File file, String content) throws IOException {
        try(OutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes());
            out.flush();
        }
    }

    private String readeFile(File file) throws IOException {
        StringBuilder result = new StringBuilder();
        try(Reader in = new FileReader(file)) {
            char [] buffer = new char[1024];
            for(int read = in.read(buffer) ; read != -1 ; read = in.read(buffer)) {
                result.append(buffer, 0, read);
            }
        }
        return result.toString();
    }


}