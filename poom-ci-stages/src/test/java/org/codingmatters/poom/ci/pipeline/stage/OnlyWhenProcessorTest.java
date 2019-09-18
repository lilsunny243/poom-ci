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
    public void branchIs() throws Exception {
        this.branch = "develop";
        assertThat(processor.isExecutable(Stage.builder().onlyWhen("branch is develop").build()), is(true));

        this.branch = "master";
        assertThat(processor.isExecutable(Stage.builder().onlyWhen("branch is develop").build()), is(false));

        this.branch = "yopyop tagada";
        assertThat(processor.isExecutable(Stage.builder().onlyWhen("branch is 'yopyop tagada'").build()), is(true));

        this.branch = "feature/deployment-#1";
        assertThat(processor.isExecutable(Stage.builder().onlyWhen("branch is feature/deployment-#1").build()), is(true));
    }

    @Test
    public void branchIn() throws Exception{
        this.branch = "develop";
        assertThat(processor.isExecutable(Stage.builder().onlyWhen("branch in (master, develop)").build()), is(true));

        this.branch = "master";
        assertThat(processor.isExecutable(Stage.builder().onlyWhen("branch in (master, develop)").build()), is(true));

        this.branch = "feature/deployment-#1";
        assertThat(processor.isExecutable(Stage.builder().onlyWhen("branch in (master, develop)").build()), is(false));

        this.branch = "feature/deployment-#1";
        assertThat(processor.isExecutable(Stage.builder().onlyWhen("branch in (master, develop, feature/deployment-#1)").build()), is(true));

        this.branch = "feature/refactor-ingredient-1.42.0-dev##56#258";
        assertThat(processor.isExecutable(Stage.builder().onlyWhen("branch in (master, develop, 'feature/refactor-ingredient-1.42.0-dev##56#258')").build()), is(true));
    }

    @Test(expected = OnlyWhenParsingException.class)
    public void parseError() throws Exception {
        processor.isExecutable(Stage.builder().onlyWhen("not quite parsable").build());
    }
}