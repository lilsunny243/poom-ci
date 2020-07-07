package org.codingmatters.poom.ci.dependency.flat.relations;

import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;

@FunctionalInterface
public interface RelationWalkerListener {
    void relates(Repository upstream, Module through, Repository downstream);
}
