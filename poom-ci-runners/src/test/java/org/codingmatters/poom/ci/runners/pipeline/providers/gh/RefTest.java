package org.codingmatters.poom.ci.runners.pipeline.providers.gh;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RefTest {
    @Test
    public void whenRefOnDevelop__thenBranchIsOneLevel() {
        assertThat(new Ref("refs/heads/develop").branch(), is("develop"));
    }
    @Test
    public void whenRefOnFeature__thenBranchIsTwoLevel() {
        assertThat(new Ref("refs/heads/feature/deployment-#1").branch(), is("feature/deployment-#1"));
    }
}