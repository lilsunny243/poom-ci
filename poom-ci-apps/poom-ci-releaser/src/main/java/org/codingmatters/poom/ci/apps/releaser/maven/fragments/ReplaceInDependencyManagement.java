package org.codingmatters.poom.ci.apps.releaser.maven.fragments;

import org.codingmatters.poom.ci.apps.releaser.maven.pom.ArtifactCoordinates;

import java.util.regex.Pattern;

public class ReplaceInDependencyManagement extends AbstractArtifactCoordinatesReplace {

    public ReplaceInDependencyManagement(String groupId, String artifactId, String version, String content) {
        super(
                Pattern.compile("(<dependencyManagement>.*?<dependencies>)(.*?)(</dependencies>.*?</dependencyManagement>)", Pattern.DOTALL),
                new ArtifactCoordinates(groupId, artifactId, version),
                content
        );
    }

    protected AbstractArtifactCoordinates createArtifactCoordinates(String fragment) {
        return new Dependencies(fragment);
    }
}
