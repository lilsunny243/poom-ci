package org.codingmatters.poom.ci.apps.releaser.maven.fragments;

import org.codingmatters.poom.ci.apps.releaser.maven.pom.ArtifactCoordinates;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AbstractArtifactCoordinates {
    static public Pattern GROUPID_PATTERN = Pattern.compile("(<groupId>\\s*)(.*?)(\\s*</groupId>)", Pattern.DOTALL);
    static public Pattern ARTIFACTID_PATTERN = Pattern.compile("(<artifactId>\\s*)(.*?)(\\s*</artifactId>)", Pattern.DOTALL);
    static public Pattern VERSION_PATTERN = Pattern.compile("(<version>\\s*)(.*?)(\\s*</version>)", Pattern.DOTALL);

    private final Pattern artifactLocatorPattern;
    private final String content;

    public AbstractArtifactCoordinates(Pattern artifactLocatorPattern, String content) {
        this.artifactLocatorPattern = artifactLocatorPattern;
        this.content = content;
    }

    public String replace(ArtifactCoordinates coordinates, ArtifactCoordinates withCoordinates) {
        StringBuilder result = new StringBuilder();
        Matcher dependencyMatcher = artifactLocatorPattern.matcher(this.content);
        int lastMatchEnd = 0;
        while (dependencyMatcher.find()) {
            result.append(this.content, lastMatchEnd, dependencyMatcher.start());
            String dependency = this.content.substring(dependencyMatcher.start(), dependencyMatcher.end());
            if (this.matches(dependency, coordinates)) {
                result.append(this.replacedDep(dependency, withCoordinates));
            } else {
                result.append(dependency);
            }

            lastMatchEnd = dependencyMatcher.end();
        }
        if (lastMatchEnd < this.content.length() - 1) {
            String after = this.content.substring(lastMatchEnd);
            result.append(after);
        }

        return result.toString();
    }

    private boolean matches(String dependency, ArtifactCoordinates coordinates) {
        Matcher groupIdMatcher = GROUPID_PATTERN.matcher(dependency);
        if (!groupIdMatcher.find()) return false;
        if (!groupIdMatcher.group(2).equals(coordinates.getGroupId())) return false;

        Matcher artifactIdMatcher = ARTIFACTID_PATTERN.matcher(dependency);
        if (!artifactIdMatcher.find()) return false;
        if (!artifactIdMatcher.group(2).equals(coordinates.getArtifactId())) return false;

        Matcher versionMatcher = VERSION_PATTERN.matcher(dependency);
        if (!versionMatcher.find()) return false;
        if (!versionMatcher.group(2).equals(coordinates.getVersion())) return false;

        return true;
    }

    private String replacedDep(String dependency, ArtifactCoordinates withCoordinates) {
        dependency = this.replacePart(dependency, GROUPID_PATTERN.matcher(dependency), withCoordinates.getGroupId());
        dependency = this.replacePart(dependency, ARTIFACTID_PATTERN.matcher(dependency), withCoordinates.getArtifactId());
        dependency = this.replacePart(dependency, VERSION_PATTERN.matcher(dependency), withCoordinates.getVersion());

        return dependency;
    }

    static public String replacePart(String in, Matcher matcher, String with) {
        matcher.find();
        in = in.substring(0, matcher.start()) +
                matcher.group(1) +
                with +
                matcher.group(3) +
                in.substring(matcher.end());
        return in;
    }
}
