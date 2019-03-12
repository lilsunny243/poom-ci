package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.List;

public class UpdateRepositoryDependenciesQuery extends AbstractUpdateRepositoryToModuleEdges {


    public UpdateRepositoryDependenciesQuery(GraphTraversalSource g) {
        super(g, "depends-on");
    }

    @Override
    protected List<Schema.ModuleSpec> current(String repositoryId) {
        return new DependenciesQuery<>(this.graph(), Schema.ModuleSpec::from).forRepository(repositoryId);
    }
}
