package org.codingmatters.poom.ci.dependency.graph;

import org.codingmatters.poom.ci.dependency.api.types.Repository;

public interface DownstreamGraph {
    Repository[] direct(Repository from);
    Repository[] dependencyTreeFirstSteps(Repository root);
}
