package org.codingmatters.poom.ci.apps.releaser.graph;

import org.codingmatters.poom.ci.apps.releaser.task.ReleaseTaskResult;

public class GraphWalkResult {
    private final ReleaseTaskResult.ExitStatus exitStatus;
    private final String message;

    public GraphWalkResult(ReleaseTaskResult.ExitStatus exitStatus, String message) {
        this.exitStatus = exitStatus;
        this.message = message;
    }

    public ReleaseTaskResult.ExitStatus exitStatus() {
        return exitStatus;
    }

    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return "GraphWalkResult{" +
                "exitStatus=" + exitStatus +
                ", message='" + message + '\'' +
                '}';
    }
}
