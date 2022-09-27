package org.codingmatters.poom.ci.apps.releaser.task;

import org.codingmatters.poom.ci.apps.releaser.maven.pom.ArtifactCoordinates;

public class ReleaseTaskResult {
    private final ExitStatus exitStatus;
    private final String message;
    private final ArtifactCoordinates releasedVersion;

    public ReleaseTaskResult(ExitStatus exitStatus, String message, ArtifactCoordinates releasedVersion) {
        this.exitStatus = exitStatus;
        this.message = message;
        this.releasedVersion = releasedVersion;
    }

    public ExitStatus exitStatus() {
        return exitStatus;
    }

    public String message() {
        return message;
    }

    public ArtifactCoordinates releasedVersion() {
        return releasedVersion;
    }

    @Override
    public String toString() {
        return "ReleaseTaskResult{" +
                "exitStatus=" + exitStatus +
                ", message='" + message + '\'' +
                ", releasedVersion=" + releasedVersion +
                '}';
    }

    public enum ExitStatus {
        SUCCESS, FAILURE
    }
}
