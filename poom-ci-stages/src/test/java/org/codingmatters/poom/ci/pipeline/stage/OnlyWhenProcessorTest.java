package org.codingmatters.poom.ci.pipeline.stage;

import org.codingmatters.poom.ci.pipeline.descriptors.Stage;
import org.codingmatters.poom.ci.pipeline.stage.onlywhen.OnlyWhenVariableProvider;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class OnlyWhenProcessorTest {

    private String branch;

    private OnlyWhenVariableProvider variableProvider = new OnlyWhenVariableProvider() {
        @Override
        public String branch() {
            return branch;
        }
    };

    private OnlyWhenProcessor processor = new OnlyWhenProcessor(this.variableProvider);

    @Test
    public void branch() throws Exception {
        this.branch = "develop";
        assertThat(processor.isExecutable(Stage.builder().onlyWen("branch is develop").build()), is(true));

        this.branch = "master";
        assertThat(processor.isExecutable(Stage.builder().onlyWen("branch is develop").build()), is(false));

        this.branch = "yopyop tagada";
        assertThat(processor.isExecutable(Stage.builder().onlyWen("branch is 'yopyop tagada'").build()), is(true));

        this.branch = "feature/deployment-#1";
        assertThat(processor.isExecutable(Stage.builder().onlyWen("branch is feature/deployment-#1").build()), is(true));
    }

    @Test(expected = OnlyWhenParsingException.class)
    public void parseError() throws Exception {
        processor.isExecutable(Stage.builder().onlyWen("not quite parsable").build());
    }
}