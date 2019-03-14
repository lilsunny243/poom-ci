package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.List;

public abstract class AbstractUpdateRepositoryToModuleEdges {

    private final GraphTraversalSource g;
    private final String edgeLabel;

    public AbstractUpdateRepositoryToModuleEdges(GraphTraversalSource g, String edgeLabel) {
        this.g = g;
        this.edgeLabel = edgeLabel;
    }

    abstract protected List<Schema.ModuleSpec> current(String repositoryId);

    protected GraphTraversalSource graph() {
        return this.g;
    }

    public void update(String repositoryId, Schema.ModuleSpec ... moduleSpecs) {
        Vertex repo = this.g.V().has("repository", "repository-id", repositoryId).next();

        this.clearEdges(repo);
        this.setupEdges(repo, moduleSpecs);

    }

    private void setupEdges(Vertex repo, Schema.ModuleSpec[] moduleSpecs) {
        if(moduleSpecs != null && moduleSpecs.length > 0) {
            GraphTraversal traversal = this.g.V(repo.id());
            for (Schema.ModuleSpec moduleSpec : moduleSpecs) {
                GraphTraversal<Vertex, Vertex> moduleQuery = this.g.V().hasLabel("module").has("spec", moduleSpec.spec).has("version", moduleSpec.version);
                if (!moduleQuery.hasNext()) {
                    this.g.addV("module")
                            .property("spec", moduleSpec.spec)
                            .property("version", moduleSpec.version).next();
                }
                traversal.addE(this.edgeLabel)
                        .to(__.V().hasLabel("module").has("spec", moduleSpec.spec).has("version", moduleSpec.version))
                        .outV()
                ;
            }
            traversal.iterate();
        }
    }

    private void clearEdges(Vertex repo) {
        this.g.V(repo).outE(this.edgeLabel).drop().iterate();
    }
}
