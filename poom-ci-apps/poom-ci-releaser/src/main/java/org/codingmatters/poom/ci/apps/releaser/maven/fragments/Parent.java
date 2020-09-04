package org.codingmatters.poom.ci.apps.releaser.maven.fragments;

import java.util.regex.Pattern;

public class Parent extends AbstractArtifactCoordinates {
    public Parent(String content) {
        super(
                Pattern.compile("<parent>.*?</parent>", Pattern.DOTALL),
                content
        );
    }
}
