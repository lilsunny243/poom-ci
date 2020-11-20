package org.codingmatters.poom.ci.apps.releaser.graph;

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

    public synchronized Pom applyTo(Pom pom) throws IOException {
        for (ArtifactCoordinates artifact : this.propagatedArtifactVersions.values()) {
            pom = this.propagate(artifact, pom);
        }
        return pom;
    }

    private Pom propagate(ArtifactCoordinates artifact, Pom pom) throws IOException {
        if(pom.parent() != null) {
            if (this.matches(artifact, pom.parent())) {
                pom = new Pom(pom.withParentVersion(artifact.getVersion()));
            }
        }
        pom = new Pom(pom.withDependencyVersion(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
        pom = new Pom(pom.withPluginVersion(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));

        return pom;
    }

    private boolean matches(ArtifactCoordinates a1, ArtifactCoordinates a2) {
        return a1.getGroupId().equals(a2.getGroupId()) && a1.getArtifactId().equals(a2.getArtifactId());
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
