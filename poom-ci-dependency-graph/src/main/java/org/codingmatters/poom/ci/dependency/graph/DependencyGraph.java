package org.codingmatters.poom.ci.dependency.graph;

import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;

import java.io.IOException;

public interface DependencyGraph extends RepositoryGraph {
    DependencyGraph add(Module... modules) throws IOException;

    DependencyGraph produces(Repository repository, Module... modules) throws IOException;

    DependencyGraph dependsOn(Repository repository, Module... modules) throws IOException;

    Module[] modules();

    Module[] produced(Repository repository);

    Repository[] depending(Module module) throws IOException;

    Module[] dependencies(Repository repository);

    Repository[] downstream(Repository repository);

    DownstreamGraph downstreamGraph(Repository repository) throws IOException;

    DependencyGraph resetDependencies(Repository repository);

    DependencyGraph resetProduced(Repository repository);
}
