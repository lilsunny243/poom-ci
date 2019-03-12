package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class CreateOrUpdateRepositoryQuery {

    private final GraphTraversalSource g;

    public CreateOrUpdateRepositoryQuery(GraphTraversalSource g) {
        this.g = g;
    }

    public void update(String repositoryId, String name, String checkoutSpec) {

        if(this.g.V().hasLabel("repository").has("repository-id", repositoryId).hasNext()) {
            this.g.V().hasLabel("repository").has("repository-id", repositoryId)
                    .property("name", name)
                    .property("checkout-spec", checkoutSpec)
                    .next();
        } else {
            this.g.addV("repository")
                    .property("repository-id", repositoryId)
                    .property("name", name)
                    .property("checkout-spec", checkoutSpec)
                    .next();
        }
    }
}
