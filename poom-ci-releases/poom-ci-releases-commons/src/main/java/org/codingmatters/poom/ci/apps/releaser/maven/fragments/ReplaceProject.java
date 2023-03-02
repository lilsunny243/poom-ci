package org.codingmatters.poom.ci.apps.releaser.maven.fragments;

import org.codingmatters.poom.ci.apps.releaser.maven.Pom;
import org.codingmatters.poom.ci.apps.releaser.maven.pom.ArtifactCoordinates;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

public class ReplaceProject {

    private final ArtifactCoordinates artifactCoordinates;
    private final String content;
    private final LinkedList<Range> ranges = new LinkedList<>();

    public ReplaceProject(String groupId, String artifactId, String version, String content) {
        this.content = content;

        Range dependencyManagementRange = Range.from(this.content, "<dependencyManagement>", "</dependencyManagement>", true);
        if(dependencyManagementRange.start() != -1) {
            ranges.add(dependencyManagementRange);
        }
        Range buildRange = Range.from(this.content, "<build>", "</build>", true);
        if(buildRange.start() != -1) {
            ranges.add(buildRange);
        }
        Range dependenciesRange;
        if(dependencyManagementRange.start() == -1) {
            dependenciesRange = Range.from(this.content, "<dependencies>", "</dependencies>", true);
        } else {
            String beforeDM = this.content.substring(0, dependencyManagementRange.start());
            if(beforeDM.contains("<dependencies>")) {
                dependenciesRange = new Range(
                        beforeDM.indexOf("<dependencies>"),
                        beforeDM.indexOf("</dependencies>") + "</dependencies>".length(),
                    true
                );
            } else {
                String afterDM = this.content.substring(dependencyManagementRange.end());
                dependenciesRange = new Range(
                        afterDM.indexOf("<dependencies>"),
                        afterDM.indexOf("</dependencies>") + "</dependencies>".length(),
                        true
                );
            }
        }
        if(dependenciesRange.start() != -1) {
            ranges.add(dependenciesRange);
        }

        Comparator<? super Range> cmp = Comparator.comparingInt(value -> value.start());
        Collections.sort(ranges, cmp);

        List<Range> gaps = new LinkedList<>();
        int start = 0;
        for (Range range : ranges) {
            if(start < range.start()) {
                gaps.add(new Range(start, range.start() - 1, false));
                start = range.end() + 1;
            }
        }
        if(start < this.content.length()) {
            this.ranges.add(new Range(start, this.content.length() - 1, false));
        }
        ranges.addAll(gaps);

        Collections.sort(ranges, cmp);

        this.artifactCoordinates = new ArtifactCoordinates(groupId, artifactId, version);
    }

    public Pom.PomSource with(String groupId, String artifactId, String version) {
        StringBuilder result = new StringBuilder();
        ArtifactCoordinates targetCoordinates = new ArtifactCoordinates(groupId, artifactId, version);

        for (Range range : this.ranges) {
            String rangeContent = range.of(this.content);
            if(! range.ignored()) {
                result.append(this.replaceIn(range.of(this.content), targetCoordinates));
            } else {
                result.append(rangeContent);
            }
        }

        return new Pom.InMemoryPomSource(result.toString());
    }

    private String replaceIn(String fragment, ArtifactCoordinates targetCoordinates) {
        String result = fragment;

        Matcher groupMatcher = AbstractArtifactCoordinates.GROUPID_PATTERN.matcher(result);
        if(groupMatcher.find()) {
            if(groupMatcher.group(2).equals(this.artifactCoordinates.getGroupId())) {
                result = result.substring(0, groupMatcher.start()) +
                        groupMatcher.group(1) + targetCoordinates.getGroupId() + groupMatcher.group(3) +
                        result.substring(groupMatcher.end());
            }
        }

        Matcher artifactMatcher = AbstractArtifactCoordinates.ARTIFACTID_PATTERN.matcher(result);
        if(artifactMatcher.find()) {
            if(artifactMatcher.group(2).equals(this.artifactCoordinates.getArtifactId())) {
                result = result.substring(0, artifactMatcher.start()) +
                        artifactMatcher.group(1) + targetCoordinates.getArtifactId() + artifactMatcher.group(3) +
                        result.substring(artifactMatcher.end());
            }
        }

        Matcher versionMatcher = AbstractArtifactCoordinates.VERSION_PATTERN.matcher(result);
        if(versionMatcher.find()) {
            if(versionMatcher.group(2).equals(this.artifactCoordinates.getVersion())) {
                result = result.substring(0, versionMatcher.start()) +
                        versionMatcher.group(1) + targetCoordinates.getVersion() + versionMatcher.group(3) +
                        result.substring(versionMatcher.end());
            }
        }

        return result;
    }

    static public class Range {

        static public Range from(String content, String start, String end, boolean ignored) {
            return new Range(
                    content.indexOf(start),
                    content.indexOf(end) + end.length(),
                    ignored
            );
        }

        private final int start;
        private final int end;
        private final boolean ignored;

        public Range(int start, int end, boolean ignored) {
            this.start = start;
            this.end = end;
            this.ignored = ignored;
        }

        public int start() {
            return start;
        }

        public int end() {
            return end;
        }

        public boolean ignored() {
            return ignored;
        }

        public String of(String content) {
            return content.substring(this.start(), this.end() + 1);
        }
    }
}
