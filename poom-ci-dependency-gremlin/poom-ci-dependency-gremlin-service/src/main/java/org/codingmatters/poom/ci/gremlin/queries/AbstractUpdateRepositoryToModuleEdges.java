package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Arrays;
import java.util.HashSet;
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

        this.removeUnexpected(repositoryId, repo, moduleSpecs);
        this.appendExpected(repo, moduleSpecs);
    }

    private void appendExpected(Vertex repo, Schema.ModuleSpec[] moduleSpecs) {
        for (Schema.ModuleSpec moduleSpec : moduleSpecs) {
            Vertex module;
            if(! this.g.V().hasLabel("module").has("spec", moduleSpec.spec).has("version", moduleSpec.version).hasNext()) {
                module = g.addV("module")
                        .property("spec", moduleSpec.spec)
                        .property("version", moduleSpec.version)
                        .next();
            } else {
                module = this.g.V().hasLabel("module").has("spec", moduleSpec.spec).has("version", moduleSpec.version).next();
            }

            if(! this.g.V(repo).outE(this.edgeLabel).where(__.inV().is(module)).hasNext()) {
                Edge e = g.V(repo).addE(this.edgeLabel).to(module).next();
            }
        }
    }

    private void removeUnexpected(String repositoryId, Vertex repo, Schema.ModuleSpec[] moduleSpecs) {
        List<Schema.ModuleSpec> original = this.current(repositoryId);
        HashSet<Schema.ModuleSpec> expected = new HashSet<>(Arrays.asList(moduleSpecs));
        for (Schema.ModuleSpec moduleSpec : original) {
            if (! expected.contains(moduleSpec)) {
                this.g.V(repo).outE(this.edgeLabel)
                        .where(__.inV().hasLabel("module").has("spec", moduleSpec.spec).has("version", moduleSpec.version))
                        .drop().iterate();
            }
        }
    }
}
