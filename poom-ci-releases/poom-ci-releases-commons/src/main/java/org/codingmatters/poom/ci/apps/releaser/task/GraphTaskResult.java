package org.codingmatters.poom.ci.apps.releaser.task;

import org.codingmatters.poom.ci.apps.releaser.graph.PropagationContext;

public class GraphTaskResult {
    private final ReleaseTaskResult.ExitStatus exitStatus;
    private final String message;
    private final PropagationContext propagationContext;

    public GraphTaskResult(ReleaseTaskResult.ExitStatus exitStatus, String message, PropagationContext propagationContext) {
        this.exitStatus = exitStatus;
        this.message = message;
        this.propagationContext = propagationContext;
    }

    public ReleaseTaskResult.ExitStatus exitStatus() {
        return exitStatus;
    }

    public String message() {
        return message;
    }

    public PropagationContext propagationContext() {
        return propagationContext;
    }
}
