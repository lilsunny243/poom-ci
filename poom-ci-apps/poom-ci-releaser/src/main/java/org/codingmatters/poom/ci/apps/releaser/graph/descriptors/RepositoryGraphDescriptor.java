package org.codingmatters.poom.ci.apps.releaser.graph.descriptors;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.apps.releaser.graph.descriptors.json.RepositoryGraphReader;

import java.io.IOException;
import java.io.InputStream;

public class RepositoryGraphDescriptor {
    static private YAMLFactory YAML_FACTORY = new YAMLFactory();

    public static RepositoryGraphDescriptor fromYaml(InputStream resource) throws IOException {
        try(JsonParser parser = YAML_FACTORY.createParser(resource)) {
            return new RepositoryGraphDescriptor(new RepositoryGraphReader().read(parser));
        }
    }

    private final RepositoryGraph graph;

    public RepositoryGraphDescriptor(RepositoryGraph graph) {
        this.graph = graph;
    }

    public RepositoryGraph graph() {
        return graph;
    }
}
