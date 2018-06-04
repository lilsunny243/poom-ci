package org.codingmatters.poom.ci.dependency.graph;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.otherV;
import static org.apache.tinkerpop.gremlin.structure.io.IoCore.graphml;

public class DependencyGraph {
    public static final String DEPENDS_ON_PREDICATE = "depends-on";
    public static final String PRODUCES_PREDICATE = "produces";
    public static final String REPOSITORY_LABEL = "repository";
    public static final String MODULE_LABEL = "module";

    private final Optional<File> backupFile;
    private final Graph graph = TinkerGraph.open();

    public DependencyGraph() throws IOException {
        this(null);
    }

    public DependencyGraph(File backupFile) throws IOException {
        this.backupFile = Optional.ofNullable(backupFile);
        if(this.backupFile.isPresent()) {
            if(! this.backupFile.get().exists()) {
                this.backupFile.get().getParentFile().mkdirs();
                this.backupFile.get().createNewFile();
            }
        }
        this.loadBackup();
    }

    public DependencyGraph add(Repository ... repositories) throws IOException {
        for (Repository repository : repositories) {
            if(! this.repositoryQuery(this.graph.traversal(), repository).hasNext()) {
                this.graph.traversal().addV(REPOSITORY_LABEL)
                        .property("id", repository.id())
                        .property("name", repository.name())
                        .property("checkoutSpec", repository.checkoutSpec())
                        .next();
                this.backup();
            }
        }
        return this;
    }

    public Repository update(Repository repository) throws IOException {
        GraphTraversal<Vertex, Vertex> repo = this.repositoryVertexById(repository.id());
        if(repo.hasNext()) {
            this.graph.traversal().V(repo.next().id())
                    .property("name", repository.name())
                    .property("checkoutSpec", repository.checkoutSpec())
                    .next();
        } else {
            this.add(repository);
        }
        return repository;
    }

    public DependencyGraph add(Module ... modules) throws IOException {
        for (Module module : modules) {
            if(! this.moduleQuery(this.graph.traversal(), module).hasNext()) {
                this.graph.traversal().addV(MODULE_LABEL)
                        .property("spec", module.spec())
                        .property("version", module.version())
                        .next();
                this.backup();
            }
        }

        return this;
    }

    public DependencyGraph produces(Repository repository, Module ... modules) throws IOException {
        return this.addPredicates(repository, PRODUCES_PREDICATE, modules);
    }

    public DependencyGraph dependsOn(Repository repository, Module ... modules) throws IOException {
        return this.addPredicates(repository, DEPENDS_ON_PREDICATE, modules);
    }

    private DependencyGraph addPredicates(Repository repository, String predicate, Module[] modules) throws IOException {
        boolean changed = false;
        this.add(repository).add(modules);

        Vertex repoVertex = this.repositoryQuery(this.graph.traversal(), repository).next();

        for (Module module : modules) {
            Vertex moduleVertex = this.moduleQuery(this.graph.traversal(), module).next();
            GraphTraversal<Vertex, Edge> existingEdge = this.graph.traversal().V(repoVertex.id()).bothE(predicate).where(otherV().hasId(moduleVertex.id()));
            if(! existingEdge.hasNext()) {
                this.graph.traversal().addE(predicate).from(repoVertex).to(moduleVertex).next();
                changed = true;
            }
        }

        if(changed) {
            this.backup();
        }
        return this;
    }

    public Optional<Repository> repositoryById(String id) {
        GraphTraversal<Vertex, Vertex> repo = this.repositoryVertexById(id);
        if(repo.hasNext()) {
            return Optional.of(this.repositoryFrom(repo.next()));
        } else {
            return Optional.empty();
        }
    }

    private GraphTraversal<Vertex, Vertex> repositoryVertexById(String id) {
        return this.graph.traversal().V().hasLabel(REPOSITORY_LABEL).has("id", id);
    }

    public Repository[] repositories() {
        List<Repository> result = new LinkedList<>();
        GraphTraversal<Vertex, Vertex> repos = this.graph.traversal().V().hasLabel(REPOSITORY_LABEL);
        while(repos.hasNext()) {
            Vertex vertex = repos.next();
            result.add(this.repositoryFrom(vertex));
        }
        return result.toArray(new Repository[result.size()]);
    }

    public Module[] modules() {
        List<Module> result = new LinkedList<>();
        GraphTraversal<Vertex, Vertex> repos = this.graph.traversal().V().hasLabel(MODULE_LABEL);
        while(repos.hasNext()) {
            Vertex vertex = repos.next();
            result.add(this.moduleFrom(vertex));
        }
        return result.toArray(new Module[result.size()]);
    }

    public Module[] produced(Repository repository) {
        List<Module> results = new LinkedList<>();
        GraphTraversal<Vertex, Vertex> targets = this.repositoryQuery(this.graph.traversal(), repository)
                .out(PRODUCES_PREDICATE).V().hasLabel(MODULE_LABEL);
        while(targets.hasNext()) {
            results.add(this.moduleFrom(targets.next()));
        }
        return results.toArray(new Module[results.size()]);
    }

    public Repository[] depending(Module module) throws IOException {
        this.add(module);
        List<Repository> results = new LinkedList<>();
        GraphTraversal<Vertex, Vertex> sources = this.moduleQuery(this.graph.traversal(), module)
                .in(DEPENDS_ON_PREDICATE).V().hasLabel(REPOSITORY_LABEL);
        while(sources.hasNext()) {
            results.add(this.repositoryFrom(sources.next()));
        }
        return results.toArray(new Repository[results.size()]);
    }

    public Module[] dependencies(Repository repository) {
        List<Module> result = new LinkedList<>();
        GraphTraversal<Vertex, Vertex> modules = this.repositoryQuery(this.graph.traversal(), repository)
                .out(DEPENDS_ON_PREDICATE).V().hasLabel(MODULE_LABEL);
        while(modules.hasNext()) {
            result.add(this.moduleFrom(modules.next()));
        }
        return result.toArray(new Module[result.size()]);
    }

    public Repository[] downstream(Repository repository) {
        List<Repository> result = new LinkedList<>();
        GraphTraversal<Vertex, Vertex> downstream = this.repositoryQuery(this.graph.traversal(), repository)
                .out(PRODUCES_PREDICATE).hasLabel(MODULE_LABEL)
                .in(DEPENDS_ON_PREDICATE).hasLabel(REPOSITORY_LABEL).as("downstream")
                .select("downstream");
        while(downstream.hasNext()) {
            Vertex next = downstream.next();
            result.add(this.repositoryFrom(next));
        }
        return result.toArray(new Repository[result.size()]);
    }


    private GraphTraversal<Vertex, Vertex> repositoryQuery(GraphTraversalSource traversal, Repository repository) {
        return traversal
                .V().hasLabel("repository")
                .has("id", repository.id())
                ;
    }


    private GraphTraversal<Vertex, Vertex> moduleQuery(GraphTraversalSource traversal, Module module) {
        return traversal
                .V().hasLabel("module")
                .has("spec", module.spec())
                .has("version", module.version());
    }

    private Repository repositoryFrom(Vertex vertex) {
        return Repository.builder()
                .id(vertex.value("id").toString())
                .name(vertex.value("name").toString())
                .checkoutSpec(vertex.value("checkoutSpec").toString())
                .build();
    }

    private Module moduleFrom(Vertex vertex) {
        return Module.builder()
                .spec(vertex.value("spec").toString())
                .version(vertex.value("version").toString())
                .build();
    }

    private void backup() throws IOException {
        if(this.backupFile.isPresent()) {
            this.graph.io(graphml()).writeGraph(this.backupFile.get().getAbsolutePath());
        }
    }

    private void loadBackup() throws IOException {
        if(this.hasBackup()) {
            this.graph.io(graphml()).readGraph(this.backupFile.get().getAbsolutePath());
        }
    }

    private boolean hasBackup() {
        return this.backupFile.isPresent() && this.backupFile.get().exists() && this.backupFile.get().length() > 0;
    }

    public DependencyGraph resetDependencies(Repository repository) {
        this.repositoryQuery(this.graph.traversal(), repository).outE(DEPENDS_ON_PREDICATE).drop().iterate();
        return this;
    }

    public DependencyGraph resetProduced(Repository repository) {
        this.repositoryQuery(this.graph.traversal(), repository).outE(PRODUCES_PREDICATE).drop().iterate();
        return this;
    }
}
