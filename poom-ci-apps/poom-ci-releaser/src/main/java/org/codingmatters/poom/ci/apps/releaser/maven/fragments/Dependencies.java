package org.codingmatters.poom.ci.apps.releaser.maven.fragments;

import java.util.regex.Pattern;

public class Dependencies extends AbstractArtifactCoordinates {
    public Dependencies(String content) {
        super(Pattern.compile("<dependency>.*?</dependency>", Pattern.DOTALL), content);
    }
}
