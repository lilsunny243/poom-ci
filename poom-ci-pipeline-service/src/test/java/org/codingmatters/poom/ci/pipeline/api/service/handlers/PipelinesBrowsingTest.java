package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelinesGetRequest;
import org.codingmatters.poom.ci.pipeline.api.ValueList;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class PipelinesBrowsingTest extends AbstractPoomCITest {

    private PipelinesBrowsing handler;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        handler = new PipelinesBrowsing(this.repository());
    }

    @Test
    public void empty() {
        ValueList<Pipeline> list = this.handler.apply(PipelinesGetRequest.builder().build()).opt().status200().payload()
                .orElseThrow(() -> new AssertionError("couldn't get pipeline list"));

        assertThat(list.toArray(), is(emptyArray()));
    }

    @Test
    public void completeList() throws Exception{
        for (int i = 0; i < 50; i++) {
            this.repository().pipelineRepository().create(Pipeline.builder().build());
        }

        ValueList<Pipeline> list = this.handler.apply(PipelinesGetRequest.builder().build()).opt().status200().payload()
                .orElseThrow(() -> new AssertionError("couldn't get pipeline list"));

        assertThat(list.toArray(), is(arrayWithSize(50)));
    }

    @Test
    public void partialList() throws Exception{
        for (int i = 0; i < 50; i++) {
            this.repository().pipelineRepository().create(Pipeline.builder()
                    .build());
        }

        ValueList<Pipeline> list = this.handler.apply(PipelinesGetRequest.builder()
                .range("10-15")
                .build()).opt().status206().payload()
                .orElseThrow(() -> new AssertionError("couldn't get pipeline list"));

        assertThat(list.toArray(), is(arrayWithSize(6)));
    }

}