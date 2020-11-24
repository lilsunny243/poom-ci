package org.codingmatters.poom.ci.apps.releaser.maven.fragments;

import org.codingmatters.poom.ci.apps.releaser.maven.pom.ArtifactCoordinates;

import java.util.regex.Pattern;

public class ReplaceInPlugins extends AbstractArtifactCoordinatesReplace {
    public ReplaceInPlugins(String groupId, String artifactId, String version, String content) {
        super(
                Pattern.compile("(<build>.*?<plugins>)(.*?)(</plugins>.*?</build>)", Pattern.DOTALL),
                new ArtifactCoordinates(groupId, artifactId, version),
                content
        );
    }

    @Override
    protected AbstractArtifactCoordinates createArtifactCoordinates(String fragment) {
        return new Plugins(fragment);
    }
}
