package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.GithubTriggerGetRequest;
import org.codingmatters.poom.ci.pipeline.api.githubtriggergetresponse.Status200;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class GithubTriggerGetTest extends AbstractPoomCITest {

    private GithubTriggerGet handler = new GithubTriggerGet(this.repository());

    @Test
    public void whenTriggerNotFound__then404() {
        this.handler.apply(GithubTriggerGetRequest.builder().triggerId("not-found").build()).opt()
                .status404()
                .orElseThrow(() -> new AssertionError("should have a 404"));
    }

    @Test
    public void whenEntityFound__then200WithTriggerPayload() throws Exception {
        Entity<GithubPushEvent> trigger = this.repository().githubPushEventRepository().create(GithubPushEvent.builder().ref("yop/yop/yop").build());

        Status200 found = this.handler.apply(GithubTriggerGetRequest.builder().triggerId(trigger.id()).build()).opt()
                .status200()
                .orElseThrow(() -> new AssertionError("should have a 200"));

        assertThat(found.payload(), is(trigger.value()));
        assertThat(found.xEntityId(), is(trigger.id()));
    }
}