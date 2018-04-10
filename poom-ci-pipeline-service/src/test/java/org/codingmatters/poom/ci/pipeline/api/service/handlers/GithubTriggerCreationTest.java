package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.GithubTriggersPostRequest;
import org.codingmatters.poom.ci.pipeline.api.githubtriggerspostresponse.Status201;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class GithubTriggerCreationTest extends AbstractPoomCITest {

    private GithubTriggerCreation handler = new GithubTriggerCreation(this.githubPushEventRepository(), this.pipelineRepository());

    @Test
    public void whenPushEventPosted__thenTriggerCreated_andPipelineCreated() throws Exception {
        Status201 creation = this.handler.apply(GithubTriggersPostRequest.builder()
                .payload(event -> event.ref("refs/heads/br1").received(LocalDateTime.now()))
                .build()).opt()
                .status201()
                .orElseThrow(() -> new AssertionError("cannot post push event"));

        assertThat(creation.xEntityId(), is(notNullValue()));
        assertThat(creation.location(), is(notNullValue()));
        assertThat(this.githubPushEventRepository().retrieve(creation.xEntityId()), is(notNullValue()));

        Entity<Pipeline> pipeline = this.pipelineRepository().all(0, 1).get(0);
        assertThat(pipeline.value().trigger().type(), is(PipelineTrigger.Type.GITHUB_PUSH));
        assertThat(pipeline.value().trigger().triggerId(), is(creation.xEntityId()));
    }
}