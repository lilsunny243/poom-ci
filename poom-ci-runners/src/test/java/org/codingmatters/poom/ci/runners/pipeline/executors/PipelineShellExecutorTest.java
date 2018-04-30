package org.codingmatters.poom.ci.runners.pipeline.executors;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.pipeline.api.GithubTriggerGetRequest;
import org.codingmatters.poom.ci.pipeline.api.GithubTriggerGetResponse;
import org.codingmatters.poom.ci.pipeline.api.PoomCIPipelineAPIHandlers;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIHandlersClient;
import org.codingmatters.poom.ci.runners.pipeline.PipelineContext;
import org.codingmatters.poom.ci.runners.pipeline.providers.GithubPipelineContextProvider;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.ci.triggers.json.GithubPushEventReader;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertThat;

public class PipelineShellExecutorTest {

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    private PoomCIPipelineAPIClient pipelineAPIClient = new PoomCIPipelineAPIHandlersClient(
            new PoomCIPipelineAPIHandlers.Builder()
                    .githubTriggerGetHandler(this::triggerGet)
                    .build(),
            Executors.newFixedThreadPool(4)
    );


    private GithubTriggerGetResponse triggerGet(GithubTriggerGetRequest request) {
        try(JsonParser parser = this.jsonFactory.createParser(this.inputStream("github-push-master.json"))) {
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

    private PipelineContext context;

    @Before
    public void setUp() throws Exception {
        System.setProperty(GithubPipelineContextProvider.PROVIDER_WORKDIR_PROP, this.dir.getRoot().getAbsolutePath());
        GithubPipelineContextProvider provider = new GithubPipelineContextProvider(this.pipelineAPIClient, this.yamlFactory);

        this.context = provider.pipelineContext(
                "pipeline-id",
                PipelineTrigger.builder().type(PipelineTrigger.Type.GITHUB_PUSH).triggerId("trigger-id").build()
        );
    }

    private List<String> logs = Collections.synchronizedList(new LinkedList<>());

    @Test
    public void stage1() throws Exception {
        new PipelineShellExecutor(this.context, null, null, jsonFactory).execute(
                this.context.pipeline().stageHolder("stage1"),
                log -> logs.add(log));
        Thread.sleep(500L);
        assertThat(logs, Matchers.hasSize(5));
    }

    @Test
    public void stage2() throws Exception {
        new PipelineShellExecutor(this.context, null, null, jsonFactory).execute(this.context.pipeline().stageHolder("stage2"), log -> logs.add(log));
        assertThat(logs, Matchers.hasSize(8));
    }

    private InputStream inputStream(String resource) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    }
}