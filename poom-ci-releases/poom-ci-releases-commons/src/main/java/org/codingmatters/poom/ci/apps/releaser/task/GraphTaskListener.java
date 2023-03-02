package org.codingmatters.poom.ci.apps.releaser.task;

import org.codingmatters.poom.ci.apps.releaser.graph.GraphWalkResult;

public interface GraphTaskListener {
    void info(GraphWalkResult result);
    void error(GraphWalkResult result);
}
