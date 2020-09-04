package org.codingmatters.poom.ci.apps.releaser.maven.pom;

import java.util.List;
import java.util.Objects;

public class DependencyManagement {
    private List<ArtifactCoordinates> dependencies;

    public List<ArtifactCoordinates> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<ArtifactCoordinates> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyManagement that = (DependencyManagement) o;
        return Objects.equals(dependencies, that.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependencies);
    }

    @Override
    public String toString() {
        return "DependencyManagement{" +
                "dependencies=" + dependencies +
                '}';
    }
}
