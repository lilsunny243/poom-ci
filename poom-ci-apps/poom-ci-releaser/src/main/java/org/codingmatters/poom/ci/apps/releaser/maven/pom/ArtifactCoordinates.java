package org.codingmatters.poom.ci.apps.releaser.maven.pom;

import java.util.Objects;

public class ArtifactCoordinates {
    private String groupId;
    private String artifactId;
    private String version;

    public ArtifactCoordinates() {
    }

    public ArtifactCoordinates(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArtifactCoordinates that = (ArtifactCoordinates) o;
        return Objects.equals(groupId, that.groupId) &&
                Objects.equals(artifactId, that.artifactId) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    @Override
    public String toString() {
        return "ArtifactCoordinatesDesc{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    public String coodinates() {
        return String.format("%s:%s:%s", this.getGroupId(), this.getArtifactId(), this.getVersion());
    }

    public boolean matches(ArtifactCoordinates a) {
        return this.getGroupId().equals(a.getGroupId()) && this.getArtifactId().equals(a.getArtifactId());
    }
}
