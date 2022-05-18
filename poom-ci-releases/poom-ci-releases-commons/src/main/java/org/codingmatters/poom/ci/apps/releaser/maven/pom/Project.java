package org.codingmatters.poom.ci.apps.releaser.maven.pom;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Project extends ArtifactCoordinates {

    private ArtifactCoordinates parent;
    private List<ArtifactCoordinates> dependencies;
    private DependencyManagement dependencyManagement;
    private Map<String, String> properties;
    private Build build;

    public ArtifactCoordinates getParent() {
        return parent;
    }

    public void setParent(ArtifactCoordinates parent) {
        this.parent = parent;
    }

    public List<ArtifactCoordinates> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<ArtifactCoordinates> dependencies) {
        this.dependencies = dependencies;
    }

    public DependencyManagement getDependencyManagement() {
        return dependencyManagement;
    }

    public void setDependencyManagement(DependencyManagement dependencyManagement) {
        this.dependencyManagement = dependencyManagement;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public Build getBuild() {
        return build;
    }

    public void setBuild(Build build) {
        this.build = build;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Project project = (Project) o;
        return Objects.equals(parent, project.parent) &&
                Objects.equals(dependencies, project.dependencies) &&
                Objects.equals(dependencyManagement, project.dependencyManagement) &&
                Objects.equals(properties, project.properties) &&
                Objects.equals(build, project.build);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), parent, dependencies, dependencyManagement, properties, build);
    }

    @Override
    public String toString() {
        return "Project{" +
                "parent=" + parent +
                ", dependencies=" + dependencies +
                ", dependencyManagement=" + dependencyManagement +
                ", properties=" + properties +
                ", build=" + build +
                "} " + super.toString();
    }
}
