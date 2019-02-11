package org.codingmatters.poom.ci.dependency.graph.tinkerpop;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;
import org.codingmatters.poom.ci.dependency.graph.DownstreamGraph;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.otherV;

public class TinkerPopDependencyGraph extends AbstractTinkerPopRepositoryGraph implements DependencyGraph {

    private final Optional<File> backupFile;

    public TinkerPopDependencyGraph() throws IOException {
        this(null);
    }

    public TinkerPopDependencyGraph(File backupFile) throws IOException {
        this.backupFile = Optional.ofNullable(backupFile);
        if(this.backupFile.isPresent()) {
            if(! this.backupFile.get().exists()) {
                this.backupFile.get().getParentFile().mkdirs();
                this.backupFile.get().createNewFile();
            }
        }
        this.loadBackup();
    }

    @Override
    public DependencyGraph add(Module... modules) throws IOException {
        for (Module module : modules) {
            if(! this.moduleQuery(this.traversal(), module).hasNext()) {
                this.traversal().addV(MODULE_LABEL)
                        .property("spec", module.spec())
                        .property("version", module.version())
                        .next();
                this.backup();
            }
        }

        return this;
    }

    @Override
    public TinkerPopDependencyGraph produces(Repository repository, Module... modules) throws IOException {
        return this.addPredicates(repository, PRODUCES_PREDICATE, modules);
    }

    @Override
    public TinkerPopDependencyGraph dependsOn(Repository repository, Module... modules) throws IOException {
        return this.addPredicates(repository, DEPENDS_ON_PREDICATE, modules);
    }

    private TinkerPopDependencyGraph addPredicates(Repository repository, String predicate, Module[] modules) throws IOException {
        boolean changed = false;
        this.add(repository);
        this.add(modules);

        Vertex repoVertex = this.repositoryQuery(this.traversal(), repository).next();

        for (Module module : modules) {
            Vertex moduleVertex = this.moduleQuery(this.traversal(), module).next();
            GraphTraversal<Vertex, Edge> existingEdge = this.traversal().V(repoVertex.id()).bothE(predicate).where(otherV().hasId(moduleVertex.id()));
            if(! existingEdge.hasNext()) {
                this.traversal().addE(predicate).from(repoVertex).to(moduleVertex).next();
                changed = true;
            }
        }

        if(changed) {
            this.backup();
        }
        return this;
    }

    @Override
    public Module[] modules() {
        List<Module> result = new LinkedList<>();
        GraphTraversal<Vertex, Vertex> repos = this.traversal().V().hasLabel(MODULE_LABEL);
        while(repos.hasNext()) {
            Vertex vertex = repos.next();
            result.add(this.moduleFrom(vertex));
        }
        return result.toArray(new Module[result.size()]);
    }

    @Override
    public Module[] produced(Repository repository) {
        Set<Module> results = new HashSet<>();
        GraphTraversal<Vertex, Vertex> targets = this.repositoryQuery(this.traversal(), repository)
                .out(PRODUCES_PREDICATE).hasLabel(MODULE_LABEL);
        while(targets.hasNext()) {
            results.add(this.moduleFrom(targets.next()));
        }
        return results.toArray(new Module[results.size()]);
    }

    @Override
    public Repository[] depending(Module module) throws IOException {
        this.add(module);
        Set<Repository> results = new HashSet<>();
        GraphTraversal<Vertex, Vertex> sources = this.moduleQuery(this.traversal(), module)
                .in(DEPENDS_ON_PREDICATE).hasLabel(REPOSITORY_LABEL);
        while(sources.hasNext()) {
            results.add(this.repositoryFrom(sources.next()));
        }
        return results.toArray(new Repository[results.size()]);
    }

    @Override
    public Module[] dependencies(Repository repository) {
        Set<Module> result = new HashSet<>();
        GraphTraversal<Vertex, Vertex> modules = this.repositoryQuery(this.traversal(), repository)
                .out(DEPENDS_ON_PREDICATE).hasLabel(MODULE_LABEL);
        while(modules.hasNext()) {
            result.add(this.moduleFrom(modules.next()));
        }
        return result.toArray(new Module[result.size()]);
    }

    @Override
    public Repository[] downstream(Repository repository) {
        Set<Repository> result = new HashSet<>();
        GraphTraversal<Vertex, Vertex> downstream = this.repositoryQuery(this.traversal(), repository)
                .out(PRODUCES_PREDICATE).hasLabel(MODULE_LABEL)
                .in(DEPENDS_ON_PREDICATE).hasLabel(REPOSITORY_LABEL).as("downstream")
                .select("downstream");
        while(downstream.hasNext()) {
            Vertex next = downstream.next();
            result.add(this.repositoryFrom(next));
        }
        result.remove(repository);
        return result.toArray(new Repository[result.size()]);
    }

    @Override
    public DownstreamGraph downstreamGraph(Repository repository) throws IOException {
        return TinkerPopDownstreamGraph.from(this, repository);
    }


    private GraphTraversal<Vertex, Vertex> moduleQuery(GraphTraversalSource traversal, Module module) {
        return traversal
                .V().hasLabel("module")
                .has("spec", module.spec())
                .has("version", module.version());
    }

    private Module moduleFrom(Vertex vertex) {
        return Module.builder()
                .spec(vertex.value("spec").toString())
                .version(vertex.value("version").toString())
                .build();
    }


    private boolean hasBackup() {
        return this.backupFile.isPresent() && this.backupFile.get().exists() && this.backupFile.get().length() > 0;
    }

    @Override
    public DependencyGraph resetDependencies(Repository repository) {
        this.repositoryQuery(this.traversal(), repository).outE(DEPENDS_ON_PREDICATE).drop().iterate();
        return this;
    }

    @Override
    public DependencyGraph resetProduced(Repository repository) {
        this.repositoryQuery(this.traversal(), repository).outE(PRODUCES_PREDICATE).drop().iterate();
        return this;
    }

    @Override
    protected void graphChanged() throws IOException {
        this.backup();
    }



    private synchronized void backup() throws IOException {
        if(this.backupFile.isPresent()) {
            this.io().writeGraph(this.backupFile.get().getAbsolutePath());
        }
    }

    private synchronized void loadBackup() throws IOException {
        if(this.hasBackup()) {
            this.io().readGraph(this.backupFile.get().getAbsolutePath());
        }
    }
}
