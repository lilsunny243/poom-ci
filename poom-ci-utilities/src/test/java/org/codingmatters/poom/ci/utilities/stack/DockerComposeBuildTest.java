package org.codingmatters.poom.ci.utilities.stack;

import org.codingmatters.poom.ci.utilities.stack.descriptors.ImageDescriptor;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DockerComposeBuildTest {

    @Test
    public void images() throws Exception {
        assertThat(
                new DockerComposeBuild(Thread.currentThread().getContextClassLoader().getResourceAsStream("image-build/test-docker-compose-build.yml"))
                        .builds(),
                arrayContaining(
                    ImageDescriptor.builder().image("codingmatters/poom-ci-runners").version("2.14.0-SNAPSHOT").build(),
                    ImageDescriptor.builder().image("localhost/codingmatters/poom-ci-runners").version("2.14.0-dev").build(),
                    ImageDescriptor.builder().image("harbor.ci.flexio.io/flexio/flexio-usage-audit-service").version("5.2").build(),
                    ImageDescriptor.builder().image("localhost:5000/codingmatters/poom-ci-runners").version("2.14.0-SNAPSHOT").build()
                )
        );

    }
}