package org.codingmatters.poom.ci.runners.pipeline.providers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.pipeline.api.GithubTriggerGetRequest;
import org.codingmatters.poom.ci.pipeline.api.GithubTriggerGetResponse;
import org.codingmatters.poom.ci.pipeline.api.PoomCIPipelineAPIHandlers;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIHandlersClient;
import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.runners.pipeline.PipelineContext;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.ci.triggers.json.GithubPushEventReader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

public class GithubPipelineContextProviderTest {
    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    private PoomCIPipelineAPIClient pipelineAPIClient = new PoomCIPipelineAPIHandlersClient(
            new PoomCIPipelineAPIHandlers.Builder()
                    .githubTriggerGetHandler(this::triggerGet)
                    .build(),
            Executors.newFixedThreadPool(4)
    );

    private GithubTriggerGetResponse triggerGet(GithubTriggerGetRequest request) {
        try(JsonParser parser = this.jsonFactory.createParser(this.inputStream("github-push-develop.json"))) {
            GithubPushEvent event = new GithubPushEventReader().read(parser);
            return GithubTriggerGetResponse.builder()
                    .status200(status -> status.xEntityId(request.triggerId()).payload(event))
                    .build();
        } catch (IOException e) {
            throw new AssertionError("should not occur...");
        }
    }

    private JsonFactory jsonFactory = new JsonFactory();
    private YAMLFactory yamlFactory = new YAMLFactory();

    GithubPipelineContextProvider provider = new GithubPipelineContextProvider(this.pipelineAPIClient, this.yamlFactory);

    @Before
    public void setUp() throws Exception {
        System.setProperty(GithubPipelineContextProvider.PROVIDER_WORKDIR_PROP, this.dir.getRoot().getAbsolutePath());
    }

    @Test
    public void whenPipelineContextCalled__thenWorkspaceIsCheckedoutAtGivenBranchAndRevision() throws Exception {

        PipelineContext context = this.provider.pipelineContext(
                "pipeline-id",
                PipelineTrigger.builder().type(PipelineTrigger.Type.GITHUB_PUSH).triggerId("trigger-id").build()
        );

        assertTrue(context.workspace().exists());
        assertTrue(context.workspace().isDirectory());
        assertTrue(new File(context.workspace(), ".git").exists());
        assertTrue(new File(context.workspace(), "poom-ci-pipeline.yaml").exists());
        assertTrue(new File(context.workspace(), "you-are-on-develop").exists());
        assertFalse(new File(context.workspace(), "develop-has-changed").exists());

        System.out.println(context.workspace().getAbsolutePath());
    }

    @Test
    public void givenPipelineContextArlreadyCalled__whenCalledAgain__aNewWorkspaceIsCreated() throws Exception {
        PipelineContext context1 = this.provider.pipelineContext(
                "pipeline-id",
                PipelineTrigger.builder().type(PipelineTrigger.Type.GITHUB_PUSH).triggerId("trigger-id").build()
        );
        PipelineContext context2 = this.provider.pipelineContext(
                "pipeline-id",
                PipelineTrigger.builder().type(PipelineTrigger.Type.GITHUB_PUSH).triggerId("trigger-id").build()
        );

        assertThat(context2.workspace().getAbsolutePath(), is(not(context1.workspace().getAbsolutePath())));
    }

    @Test
    public void givenPipelineContextCreated__whenPipelineFileExists__thenContextPipelineIsCreated() throws Exception {
        PipelineContext context = this.provider.pipelineContext(
                "pipeline-id",
                PipelineTrigger.builder().type(PipelineTrigger.Type.GITHUB_PUSH).triggerId("trigger-id").build()
        );

        assertThat(context.pipeline(), is(Pipeline.builder()
                .stages(
                        stage -> stage.name("stage1").exec("ls"),
                        stage -> stage.name("stage2").exec("ls -l")
                )
                .build()));
    }

    private InputStream inputStream(String resource) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    }
}