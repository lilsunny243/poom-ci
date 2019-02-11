package org.codingmatters.poom.ci.dependency.api.service.utils;

import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;
import org.codingmatters.poom.ci.dependency.graph.DownstreamGraph;

import java.io.IOException;
import java.util.Optional;

public class SynchronizedDependencyGraph implements DependencyGraph {

    private DependencyGraph dependencyGraph;

    public SynchronizedDependencyGraph(DependencyGraph dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }

    @Override
    public synchronized DependencyGraph add(Module... modules) throws IOException {
        return dependencyGraph.add(modules);
    }

    @Override
    public synchronized DependencyGraph produces(Repository repository, Module... modules) throws IOException {
        return dependencyGraph.produces(repository, modules);
    }

    @Override
    public synchronized DependencyGraph dependsOn(Repository repository, Module... modules) throws IOException {
        return dependencyGraph.dependsOn(repository, modules);
    }

    @Override
    public synchronized Module[] modules() {
        return dependencyGraph.modules();
    }

    @Override
    public synchronized Module[] produced(Repository repository) {
        return dependencyGraph.produced(repository);
    }

    @Override
    public synchronized Repository[] depending(Module module) throws IOException {
        return dependencyGraph.depending(module);
    }

    @Override
    public synchronized Module[] dependencies(Repository repository) {
        return dependencyGraph.dependencies(repository);
    }

    @Override
    public synchronized Repository[] downstream(Repository repository) {
        return dependencyGraph.downstream(repository);
    }

    @Override
    public synchronized DownstreamGraph downstreamGraph(Repository repository) throws IOException {
        return dependencyGraph.downstreamGraph(repository);
    }

    @Override
    public synchronized DependencyGraph resetDependencies(Repository repository) {
        return dependencyGraph.resetDependencies(repository);
    }

    @Override
    public synchronized DependencyGraph resetProduced(Repository repository) {
        return dependencyGraph.resetProduced(repository);
    }

    @Override
    public synchronized void add(Repository... repositories) throws IOException {
        dependencyGraph.add(repositories);
    }

    @Override
    public synchronized void remove(Repository repo) throws IOException {
        dependencyGraph.remove(repo);
    }

    @Override
    public synchronized Repository update(Repository repository) throws IOException {
        return dependencyGraph.update(repository);
    }

    @Override
    public synchronized Optional<Repository> repositoryById(String id) {
        return dependencyGraph.repositoryById(id);
    }

    @Override
    public synchronized Repository[] repositories() {
        return dependencyGraph.repositories();
    }
}
