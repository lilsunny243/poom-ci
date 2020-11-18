package org.codingmatters.poom.ci.apps.releaser.maven;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.codingmatters.poom.ci.apps.releaser.maven.pom.ArtifactCoordinates;
import org.codingmatters.poom.ci.apps.releaser.maven.pom.Project;

import java.io.*;
import java.util.Optional;

public class Pom {

    static private ObjectMapper MAPPER = new XmlMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final PomSource source;
    private final Project descriptor;
    private final PomFragments fragments;

    public Pom(PomSource source) throws IOException {
        this.source = source;
        this.fragments = new PomFragments(source);
        try(Reader reader = this.source.reader()) {
            this.descriptor = MAPPER.readValue(reader, Project.class);
        }
    }

    public ArtifactCoordinates parent() {
        return this.descriptor.getParent();
    }

    public ArtifactCoordinates project() {
        return this.descriptor;
    }

    public PomSource withVersion(String newVersion) throws IOException {
        return this.fragments.replaceProject(this.project().getGroupId(), this.project().getArtifactId(), this.project().getVersion())
                .with(this.project().getGroupId(), this.project().getArtifactId(), newVersion);
    }

    public PomSource withParentVersion(String newVersion) throws IOException {
        if(this.parent() != null) {
            return this.fragments.replaceParent(this.parent().getGroupId(), this.parent().getArtifactId(), this.parent().getVersion())
                    .with(this.parent().getGroupId(), this.parent().getArtifactId(), newVersion);
        } else {
            return this.source;
        }
    }

    public PomSource withDependencyVersion(String groupId, String artifactId, String newVersion) throws IOException {
        Optional<String> version = this.rawDependencyVersion(groupId, artifactId);
        if(version.isPresent()) {
            if(this.versionIsAProperty(version.get())) {
                Optional<String> property = this.property(this.extractPropertyName(version.get()));
                if(property.isPresent()) {
                    return this.fragments.replaceProperty(this.extractPropertyName(version.get()), property.get()).with(newVersion);
                } else {
                    return this.source;
                }
            } else {
                PomFragments firstPass = new PomFragments(this.fragments.replaceDependency(groupId, artifactId, version.get()).with(groupId, artifactId, newVersion));
                return firstPass.replaceDependencyInManagement(groupId, artifactId, version.get()).with(groupId, artifactId, newVersion);
            }
        } else {
            return this.source;
        }
    }

    public PomSource withPluginVersion(String groupId, String pluginId, String newVersion) throws IOException {
        Optional<String> version = this.rawPluginVersion(groupId, pluginId);
        if(version.isPresent()) {
            if(this.versionIsAProperty(version.get())) {
                Optional<String> property = this.property(this.extractPropertyName(version.get()));
                if(property.isPresent()) {
                    return this.fragments.replaceProperty(this.extractPropertyName(version.get()), property.get()).with(newVersion);
                } else {
                    return this.source;
                }
            } else {
                PomFragments firstPass = new PomFragments(this.fragments.replacePlugin(groupId, pluginId, version.get()).with(groupId, pluginId, newVersion));
                return firstPass.replacePluginInManagement(groupId, pluginId, version.get()).with(groupId, pluginId, newVersion);
            }
        } else {
            return this.source;
        }
    }



    public Optional<String> dependencyVersion(String groupId, String artifactId) {
        Optional<String> version = this.rawDependencyVersion(groupId, artifactId);
        if(! version.isPresent()) return version;

        if(this.versionIsAProperty(version.get())) {
            Optional<String> versionProperty = this.property(this.extractPropertyName(version.get()));
            if (versionProperty.isPresent()) {
                version = versionProperty;
            }
        }

        return version;
    }

    private String extractPropertyName(String version) {
        return version.substring(2, version.length() - 1);
    }

    private boolean versionIsAProperty(String version) {
        return version.matches("\\$\\{.*\\}");
    }

    private Optional<String> rawDependencyVersion(String groupId, String artifactId) {
        Optional<String> version = this.dependencyManagementVersion(groupId, artifactId);
        if(! version.isPresent()) {
            version = this.directDependencyVersion(groupId, artifactId);
        }
        return version;
    }

    public Optional<String> directDependencyVersion(String groupId, String artifactId) {

        if(this.descriptor.getDependencies() == null) return Optional.empty();

        for (ArtifactCoordinates dependency : this.descriptor.getDependencies()) {
            if(groupId.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId()) && dependency.getVersion() != null) {
                return Optional.of(dependency.getVersion());
            }
        }
        return Optional.empty();
    }

    private Optional<String> dependencyManagementVersion(String groupId, String artifactId) {
        if(this.descriptor.getDependencyManagement() == null) return Optional.empty();
        if(this.descriptor.getDependencyManagement().getDependencies() == null) return Optional.empty();

        for (ArtifactCoordinates dependency : this.descriptor.getDependencyManagement().getDependencies()) {
            if(groupId.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId()) && dependency.getVersion() != null) {
                return Optional.of(dependency.getVersion());
            }
        }

        return Optional.empty();
    }

    public Optional<String> property(String name) {
        if(this.descriptor.getProperties() == null) return Optional.empty();
        return Optional.ofNullable(this.descriptor.getProperties().get(name));
    }

    public Optional<String> pluginVersion(String groupId, String pluginId) {
        Optional<String> version = this.rawPluginVersion(groupId, pluginId);
        if(version.isPresent() && this.versionIsAProperty(version.get())) {
            version = this.property(this.extractPropertyName(version.get()));
        }
        return version;
    }

    public void writeTo(Writer writer) throws IOException {
        try(Reader reader = this.source.reader()) {
            char[] buffer = new char[1024];
            for (int read = reader.read(buffer) ; read != -1 ; read = reader.read(buffer)) {
                writer.write(buffer, 0, read);
            }
        }
    }


    private Optional<String> rawPluginVersion(String groupId, String pluginId) {
        Optional<String> version = this.pluginManagementVersion(groupId, pluginId);
        if(! version.isPresent()) {
            version = this.directPluginVersion(groupId, pluginId);
        }
        return version;
    }

    private Optional<String> directPluginVersion(String groupId, String pluginId) {
        if(this.descriptor.getBuild() != null && this.descriptor.getBuild().getPlugins() != null) {
            for (ArtifactCoordinates plugin : this.descriptor.getBuild().getPlugins()) {
                if(groupId.equals(plugin.getGroupId()) && pluginId.equals(plugin.getArtifactId()) && plugin.getVersion() != null) {
                    return Optional.of(plugin.getVersion());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> pluginManagementVersion(String groupId, String pluginId) {
        if(this.descriptor.getBuild() != null && this.descriptor.getBuild().getPluginManagement() != null && this.descriptor.getBuild().getPluginManagement().getPlugins() != null) {
            for (ArtifactCoordinates plugin : this.descriptor.getBuild().getPluginManagement().getPlugins()) {
                if(groupId.equals(plugin.getGroupId()) && pluginId.equals(plugin.getArtifactId()) && plugin.getVersion() != null) {
                    return Optional.of(plugin.getVersion());
                }
            }
        }
        return Optional.empty();
    }


    static public Pom from(InputStream input) throws IOException {
        try(Reader reader = new InputStreamReader(input)) {
            return new Pom(new InMemoryPomSource(readString(reader)));
        }

    }

    static private String readString(Reader reader) throws IOException {
        StringBuilder result = new StringBuilder();
        char[] buffer = new char[1024];
        for(int read = reader.read(buffer); read != -1; read = reader.read(buffer)) {
            result.append(buffer, 0, read);
        }
        return result.toString();
    }

    @FunctionalInterface
    public interface PomSource {
        Reader reader() throws IOException;
    }

    static public class InMemoryPomSource implements PomSource {
        private final String content;

        public InMemoryPomSource(String content) {
            this.content = content;
        }

        @Override
        public Reader reader() throws IOException {
            return new StringReader(this.content);
        }
    }
}
