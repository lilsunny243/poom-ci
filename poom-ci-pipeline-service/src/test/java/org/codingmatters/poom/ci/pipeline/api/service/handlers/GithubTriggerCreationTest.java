package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.GithubTriggersPostRequest;
import org.codingmatters.poom.ci.pipeline.api.githubtriggerspostresponse.Status201;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class GithubTriggerCreationTest extends AbstractPoomCITest {

    private AtomicReference<PipelineTrigger> lastTriggered = new AtomicReference<>(null);

    private GithubTriggerCreation handler;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        handler = new GithubTriggerCreation(this.repository(), pipelineTrigger -> lastTriggered.set(pipelineTrigger));
    }

    @Test
    public void whenPushEventPosted__thenTriggerCreated_andPipelineCreated() throws Exception {
        Status201 creation = this.handler.apply(GithubTriggersPostRequest.builder()
                .payload(event -> event.ref("refs/heads/br1").received(LocalDateTime.now()))
                .build()).opt()
                .status201()
                .orElseThrow(() -> new AssertionError("cannot post push event"));

        assertThat(creation.xEntityId(), is(notNullValue()));
        assertThat(creation.location(), is(notNullValue()));
        assertThat(this.repository().githubPushEventRepository().retrieve(creation.xEntityId()), is(notNullValue()));

        assertThat(this.lastTriggered.get().type(), is(PipelineTrigger.Type.GITHUB_PUSH));
        assertThat(this.lastTriggered.get().triggerId(), is(creation.xEntityId()));
    }
}