package org.codingmatters.poom.ci.dependency.graph;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.graphml.GraphMLIo;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.codingmatters.poom.ci.dependency.api.types.Repository;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.apache.tinkerpop.gremlin.structure.io.IoCore.graphml;

public abstract class AbstractRepositoryGraph<G extends AbstractRepositoryGraph> {
    public static final String DEPENDS_ON_PREDICATE = "depends-on";
    public static final String PRODUCES_PREDICATE = "produces";
    public static final String DOWNSTREAM_PREDICATE = "downstream";

    public static final String REPOSITORY_LABEL = "repository";
    public static final String MODULE_LABEL = "module";

    private final Graph graph = TinkerGraph.open();

    protected abstract void graphChanged() throws IOException;


    public G add(Repository... repositories) throws IOException {
        for (Repository repository : repositories) {
            if(! this.repositoryQuery(this.traversal(), repository).hasNext()) {
                this.traversal().addV(REPOSITORY_LABEL)
                        .property("id", repository.id())
                        .property("name", repository.name())
                        .property("checkoutSpec", repository.checkoutSpec())
                        .next();
                this.graphChanged();
            }
        }
        return (G) this;
    }

    public Repository update(Repository repository) throws IOException {
        GraphTraversal<Vertex, Vertex> repo = this.repositoryVertexById(repository.id());
        if(repo.hasNext()) {
            this.traversal().V(repo.next().id())
                    .property("id", repository.id())
                    .property("name", repository.name())
                    .property("checkoutSpec", repository.checkoutSpec())
                    .next();
        } else {
            this.add(repository);
        }
        return repository;
    }

    public Optional<Repository> repositoryById(String id) {
        GraphTraversal<Vertex, Vertex> repo = this.repositoryVertexById(id);
        if(repo.hasNext()) {
            return Optional.of(this.repositoryFrom(repo.next()));
        } else {
            return Optional.empty();
        }
    }

    public Repository[] repositories() {
        List<Repository> result = new LinkedList<>();
        GraphTraversal<Vertex, Vertex> repos = this.traversal().V().hasLabel(REPOSITORY_LABEL);
        while(repos.hasNext()) {
            Vertex vertex = repos.next();
            result.add(this.repositoryFrom(vertex));
        }
        return result.toArray(new Repository[result.size()]);
    }

    protected GraphTraversalSource traversal() {
        return this.graph.traversal();
    }

    protected GraphTraversal<Vertex, Vertex> repositoryQuery(GraphTraversalSource traversal, Repository repository) {
        return traversal
                .V().hasLabel("repository")
                .has("id", repository.id())
                ;
    }

    protected GraphTraversal<Vertex, Vertex> repositoryVertexById(String id) {
        return this.traversal().V().hasLabel(REPOSITORY_LABEL).has("id", id);
    }

    protected Repository repositoryFrom(Vertex vertex) {
        return Repository.builder()
                .id(vertex.value("id").toString())
                .name(vertex.value("name").toString())
                .checkoutSpec(vertex.value("checkoutSpec").toString())
                .build();
    }

    protected GraphMLIo io() {
        return this.graph.io(graphml());
    }
}
