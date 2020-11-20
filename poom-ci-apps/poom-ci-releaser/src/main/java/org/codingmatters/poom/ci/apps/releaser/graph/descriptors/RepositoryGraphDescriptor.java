package org.codingmatters.poom.ci.apps.releaser.graph.descriptors;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.apps.releaser.graph.descriptors.json.RepositoryGraphReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class RepositoryGraphDescriptor {
    static private YAMLFactory YAML_FACTORY = new YAMLFactory();

    public static RepositoryGraphDescriptor fromYaml(InputStream resource) throws IOException {
        try(JsonParser parser = YAML_FACTORY.createParser(resource)) {
            return new RepositoryGraphDescriptor(new RepositoryGraphReader().read(parser));
        }
    }

    private final RepositoryGraph graph;

    public RepositoryGraphDescriptor(RepositoryGraph graph) {
        this.graph = graph != null ? graph : RepositoryGraph.builder().build();
    }

    public RepositoryGraph graph() {
        return graph;
    }

    public RepositoryGraphDescriptor subgraph(String startingAt) {
        return new RepositoryGraphDescriptor(this.filteredGraph(this.graph, startingAt));
    }

    private RepositoryGraph filteredGraph(RepositoryGraph g, String startingAt) {
        if(g.opt().repositories().isPresent()) {
            List<String> filteredRepositories = null;
            for (String repository : g.repositories()) {
                if(repository.equals(startingAt)) {
                    filteredRepositories = new LinkedList<>();
                }
                if(filteredRepositories != null) {
                    filteredRepositories.add(repository);
                }
            }
            if(filteredRepositories != null && ! filteredRepositories.isEmpty()) {
                return RepositoryGraph.builder()
                        .repositories(filteredRepositories)
                        .then(g.then())
                        .build();
            }
        }

        if(g.opt().then().isPresent()) {
            for (RepositoryGraph repositoryGraph : g.then()) {
                RepositoryGraph subgraph = this.filteredGraph(repositoryGraph, startingAt);
                if(subgraph != null) {
                    return subgraph;
                }
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepositoryGraphDescriptor that = (RepositoryGraphDescriptor) o;
        return Objects.equals(graph, that.graph);
    }

    @Override
    public int hashCode() {
        return Objects.hash(graph);
    }

    @Override
    public String toString() {
        return "RepositoryGraphDescriptor{" +
                "graph=" + graph +
                '}';
    }

    public boolean containsRepository(String repository) {
        return this.containsRepository(repository, this.graph);
    }

    private boolean containsRepository(String repository, RepositoryGraph g) {
        if(g.opt().repositories().isPresent() && g.repositories().contains(repository)) {
            return true;
        } else {
            if(g.opt().then().isPresent()) {
                for (RepositoryGraph repositoryGraph : g.then()) {
                    if(this.containsRepository(repository, repositoryGraph)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
