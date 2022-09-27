package org.codingmatters.poom.ci.apps.releaser.maven.fragments;

import org.codingmatters.poom.ci.apps.releaser.maven.Pom;
import org.codingmatters.poom.ci.apps.releaser.maven.pom.ArtifactCoordinates;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractArtifactCoordinatesReplace {

    private final Pattern dependenciesLocatorPattern;
    private final ArtifactCoordinates coordinates;
    private final String content;

    public AbstractArtifactCoordinatesReplace(String groupId, String artifactId, String version, String content) {
        this(Pattern.compile("(<dependencies>)(.*?)(</dependencies>)", Pattern.DOTALL), new ArtifactCoordinates(groupId, artifactId, version), content);
    }

    protected AbstractArtifactCoordinatesReplace(Pattern dependenciesLocatorPattern, ArtifactCoordinates coordinates, String content) {
        this.dependenciesLocatorPattern = dependenciesLocatorPattern;
        this.coordinates = coordinates;
        this.content = content;
    }


    public Pom.PomSource with(String targetGroupId, String targetArtifactId, String targetVersion) {
        ArtifactCoordinates targetCoordinates = new ArtifactCoordinates(targetGroupId, targetArtifactId, targetVersion);
        Matcher dependenciesMatcher = this.dependenciesLocatorPattern.matcher(this.content);
        if(dependenciesMatcher.find()) {
            return new Pom.InMemoryPomSource(
                    this.content.substring(0, dependenciesMatcher.start()) +
                            dependenciesMatcher.group(1) +
                            this.createArtifactCoordinates(dependenciesMatcher.group(2)).replace(this.coordinates, targetCoordinates) +
                            dependenciesMatcher.group(3) +
                            this.content.substring(dependenciesMatcher.end())
                    );
        }

        return new Pom.InMemoryPomSource(this.content);
    }

    abstract protected AbstractArtifactCoordinates createArtifactCoordinates(String fragment);


}
