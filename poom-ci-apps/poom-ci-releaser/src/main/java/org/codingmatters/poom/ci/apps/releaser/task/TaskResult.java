package org.codingmatters.poom.ci.apps.releaser.task;

public class TaskResult {
    private final ExitStatus exitStatus;
    private final String message;

    public TaskResult(ExitStatus exitStatus, String message) {
        this.exitStatus = exitStatus;
        this.message = message;
    }

    public ExitStatus exitStatus() {
        return exitStatus;
    }

    public String message() {
        return message;
    }

    public enum ExitStatus {
        SUCCESS, FAILURE
    }
}
