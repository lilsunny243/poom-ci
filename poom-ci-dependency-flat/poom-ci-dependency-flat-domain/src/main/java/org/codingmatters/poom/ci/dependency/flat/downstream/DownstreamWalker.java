package org.codingmatters.poom.ci.dependency.flat.downstream;

import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.flat.*;

import java.util.HashSet;
import java.util.Optional;

public class DownstreamWalker extends RelationProcessorWalker {
    public DownstreamWalker(GraphManager graphManager, DownstreamWalkerListener listener) {
        super(graphManager, new DownstreamProcessor(graphManager), listener);
    }
}
