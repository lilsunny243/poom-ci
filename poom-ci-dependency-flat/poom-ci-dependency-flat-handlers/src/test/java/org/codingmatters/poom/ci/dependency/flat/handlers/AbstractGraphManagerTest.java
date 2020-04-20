package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.DependsOnRelation;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.ProducesRelation;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;

public abstract class AbstractGraphManagerTest {

    private GraphManager graphManager = new GraphManager(
            InMemoryRepositoryWithPropertyQuery.validating(Repository.class),
            InMemoryRepositoryWithPropertyQuery.validating(ProducesRelation.class),
            InMemoryRepositoryWithPropertyQuery.validating(DependsOnRelation.class)
    );

    public GraphManager graphManager() {
        return graphManager;
    }
}
