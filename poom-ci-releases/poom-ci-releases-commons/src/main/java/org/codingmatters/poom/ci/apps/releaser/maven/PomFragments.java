package org.codingmatters.poom.ci.apps.releaser.maven;

import org.codingmatters.poom.ci.apps.releaser.maven.fragments.*;

import java.io.IOException;
import java.io.Reader;

public class PomFragments {

    private final String content;

    public PomFragments(Pom.PomSource source) throws IOException {
        try(Reader reader = source.reader()) {
            StringBuilder contentBuilder = new StringBuilder();
            char[] buffer = new char[1024];
            for(int read = reader.read(buffer) ; read != -1 ; read = reader.read(buffer)) {
                contentBuilder.append(buffer, 0, read);
            }
            this.content = contentBuilder.toString();
        }
    }


    public ReplaceInDependencies replaceDependency(String groupId, String artifactId, String version) {
        return new ReplaceInDependencies(groupId, artifactId, version, this.content);
    }

    public ReplaceInDependencyManagement replaceDependencyInManagement(String groupId, String artifactId, String version) {
        return new ReplaceInDependencyManagement(groupId, artifactId, version, this.content);
    }

    public ReplaceInPlugins replacePlugin(String groupId, String artifactId, String version) {
        return new ReplaceInPlugins(groupId, artifactId, version, this.content);
    }

    public ReplaceInPluginManagement replacePluginInManagement(String groupId, String artifactId, String version) {
        return new ReplaceInPluginManagement(groupId, artifactId, version, this.content);
    }

    public ReplaceProperty replaceProperty(String name, String value) {
        return new ReplaceProperty(name, value, this.content);
    }

    public ReplaceParent replaceParent(String groupId, String artifactId, String version) {
        return new ReplaceParent(groupId, artifactId, version, this.content);
    }

    public ReplaceProject replaceProject(String groupId, String artifactId, String version) {
        return new ReplaceProject(groupId, artifactId, version, this.content);
    }
}
