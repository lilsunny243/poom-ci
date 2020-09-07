package org.codingmatters.poom.ci.dependency.flat.downstream;

import org.codingmatters.poom.ci.dependency.api.types.Repository;

@FunctionalInterface
public interface DownstreamWalkerListener {
    void hasRelated(Repository parent, Repository related, boolean cycleInduced);
}
