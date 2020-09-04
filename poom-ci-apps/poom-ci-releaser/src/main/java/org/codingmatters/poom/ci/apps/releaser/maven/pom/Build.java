package org.codingmatters.poom.ci.apps.releaser.maven.pom;

import java.util.List;
import java.util.Objects;

public class Build {
    private List<ArtifactCoordinates> plugins;
    private PluginManagement pluginManagement;

    public List<ArtifactCoordinates> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<ArtifactCoordinates> plugins) {
        this.plugins = plugins;
    }

    public PluginManagement getPluginManagement() {
        return pluginManagement;
    }

    public void setPluginManagement(PluginManagement pluginManagement) {
        this.pluginManagement = pluginManagement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Build build = (Build) o;
        return Objects.equals(plugins, build.plugins) &&
                Objects.equals(pluginManagement, build.pluginManagement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plugins, pluginManagement);
    }

    @Override
    public String toString() {
        return "Build{" +
                "plugins=" + plugins +
                ", pluginManagement=" + pluginManagement +
                '}';
    }
}
