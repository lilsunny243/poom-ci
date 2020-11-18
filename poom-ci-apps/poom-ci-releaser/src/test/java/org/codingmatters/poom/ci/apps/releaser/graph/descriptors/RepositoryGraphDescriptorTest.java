package org.codingmatters.poom.ci.apps.releaser.graph.descriptors;

import org.junit.Test;

import static org.junit.Assert.*;

public class RepositoryGraphDescriptorTest {
    @Test
    public void given__when__then() throws Exception {
        RepositoryGraphDescriptor descriptor = RepositoryGraphDescriptor.fromYaml(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("graphs/a-graph.yml")
        );
        System.out.println(descriptor.graph());
    }
}