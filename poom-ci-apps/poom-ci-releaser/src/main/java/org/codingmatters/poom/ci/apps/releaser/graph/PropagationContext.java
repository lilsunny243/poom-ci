package org.codingmatters.poom.ci.apps.releaser.graph;

import org.codingmatters.poom.ci.apps.releaser.ProjectDescriptor;
import org.codingmatters.poom.ci.apps.releaser.maven.Pom;
import org.codingmatters.poom.ci.apps.releaser.maven.pom.ArtifactCoordinates;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class PropagationContext {

    private final Map<String, ArtifactCoordinates> propagatedArtifactVersions = new LinkedHashMap<>();

    public synchronized void addPropagatedArtifact(ArtifactCoordinates propagated) {
        this.propagatedArtifactVersions.put(propagated.getGroupId() + ":" + propagated.getArtifactId(), propagated);
    }

    public synchronized ProjectDescriptor applyTo(ProjectDescriptor pom) throws IOException {
        for (ArtifactCoordinates artifact : this.propagatedArtifactVersions.values()) {
            pom = this.propagate(artifact, pom);
        }
        return pom;
    }

    private ProjectDescriptor propagate(ArtifactCoordinates artifact, ProjectDescriptor pom) throws IOException {
        return pom.changeVersion(artifact);
    }


    public synchronized boolean iEmpty() {
        return this.propagatedArtifactVersions.isEmpty();
    }

    public synchronized String text() {
        StringBuilder result = new StringBuilder();
        for (ArtifactCoordinates coordinates : this.propagatedArtifactVersions.values()) {
            result.append(" - ")
                    .append(coordinates.getGroupId()).append(":").append(coordinates.getArtifactId()).append(":").append(coordinates.getVersion())
                    .append("\n");
        }

        return result.toString();
    }
}
