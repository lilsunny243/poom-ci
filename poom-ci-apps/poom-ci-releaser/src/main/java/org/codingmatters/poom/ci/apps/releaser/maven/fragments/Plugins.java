package org.codingmatters.poom.ci.apps.releaser.maven.fragments;

import java.util.regex.Pattern;

public class Plugins extends AbstractArtifactCoordinates {
    public Plugins(String content) {
        super(
                Pattern.compile("<plugin>.*?</plugin>", Pattern.DOTALL),
                content
        );
    }
}
