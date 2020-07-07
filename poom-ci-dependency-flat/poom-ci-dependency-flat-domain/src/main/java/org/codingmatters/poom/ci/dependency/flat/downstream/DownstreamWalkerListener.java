package org.codingmatters.poom.ci.dependency.flat.downstream;

import org.codingmatters.poom.ci.dependency.api.types.Repository;

@FunctionalInterface
public interface DownstreamWalkerListener {
    void hasDownstream(Repository parent,Repository downstream, boolean cycleInduced);
}
