package org.codingmatters.poom.ci.apps.releaser.maven.pom;

import java.util.List;
import java.util.Objects;

public class PluginManagement {
    private List<ArtifactCoordinates> plugins;

    public List<ArtifactCoordinates> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<ArtifactCoordinates> plugins) {
        this.plugins = plugins;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginManagement that = (PluginManagement) o;
        return Objects.equals(plugins, that.plugins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plugins);
    }

    @Override
    public String toString() {
        return "PluginManagement{" +
                "plugins=" + plugins +
                '}';
    }
}
