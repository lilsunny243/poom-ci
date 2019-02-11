package org.codingmatters.poom.ci.dependency.graph;

import org.apache.tinkerpop.gremlin.structure.io.graphml.GraphMLIo;
import org.codingmatters.poom.ci.dependency.api.types.Repository;

import java.io.IOException;
import java.util.Optional;

public interface RepositoryGraph {
    void add(Repository... repositories) throws IOException;

    void remove(Repository repo) throws IOException;

    Repository update(Repository repository) throws IOException;

    Optional<Repository> repositoryById(String id);

    Repository[] repositories();
}
