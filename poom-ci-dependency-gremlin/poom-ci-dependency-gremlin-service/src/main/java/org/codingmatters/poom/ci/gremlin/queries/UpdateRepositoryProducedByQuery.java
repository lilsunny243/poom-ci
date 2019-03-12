package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.List;

public class UpdateRepositoryProducedByQuery extends AbstractUpdateRepositoryToModuleEdges {

    public UpdateRepositoryProducedByQuery(GraphTraversalSource g) {
        super(g, "produces");
    }

    @Override
    protected List<Schema.ModuleSpec> current(String repositoryId) {
        return new ProducedByQuery<Schema.ModuleSpec>(this.graph(), Schema.ModuleSpec::from).forRepository(repositoryId);
    }
}
