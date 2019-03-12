package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.GithubTriggersGetRequest;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class GithubTriggersBrowsingTest extends AbstractPoomCITest {

    private GithubTriggersBrowsing handler;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        handler = new GithubTriggersBrowsing(this.repository());
    }

    @Test
    public void empty() {
        assertThat(this.handler.apply(GithubTriggersGetRequest.builder().build()).opt()
                .status200()
                .orElseThrow(() -> new AssertionError("should have an empty list"))
                .payload().toArray(),
                is(emptyArray())
        );
    }

    @Test
    public void complete() throws Exception {
        for (int i = 0; i < 50; i++) {
            this.repository().githubPushEventRepository().create(GithubPushEvent.builder().ref("" + i).build());
        }
        assertThat(this.handler.apply(GithubTriggersGetRequest.builder().build()).opt()
                .status200()
                .orElseThrow(() -> new AssertionError("should have a complete list"))
                .payload().size(),
                is(50)
        );
    }

    @Test
    public void partial() throws Exception {
        for (int i = 0; i < 50; i++) {
            this.repository().githubPushEventRepository().create(GithubPushEvent.builder().ref("" + i).build());
        }
        assertThat(this.handler.apply(GithubTriggersGetRequest.builder().range("10-15").build()).opt()
                .status206()
                .orElseThrow(() -> new AssertionError("should have a partiallist"))
                .payload().size(),
                is(6)
        );
    }
}