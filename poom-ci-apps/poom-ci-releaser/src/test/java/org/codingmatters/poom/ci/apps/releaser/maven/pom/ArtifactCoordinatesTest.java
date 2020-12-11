package org.codingmatters.poom.ci.apps.releaser.maven.pom;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ArtifactCoordinatesTest {

    @Test
    public void givenMatching__whenGroupAndArtifactSetted_andGroupAndArtifactMatch_andVersionDiffers__thenMatches() throws Exception {
        assertThat(
                new ArtifactCoordinates("a", "b", "1").matches(new ArtifactCoordinates("a", "b", "2")),
                is(true)
        );
    }

    @Test
    public void givenMatching__whenGroupAndArtifactSetted_andGroupAndArtifactMatch_andVersionMatches__thenMatches() throws Exception {
        assertThat(
                new ArtifactCoordinates("a", "b", "1").matches(new ArtifactCoordinates("a", "b", "1")),
                is(true)
        );
    }

    @Test
    public void givenMatching__whenGroupAndArtifactSetted_andGroupAndVersionMatch_andArtifactDiffers__thenDoesntMatch() throws Exception {
        assertThat(
                new ArtifactCoordinates("a", "b", "1").matches(new ArtifactCoordinates("a", "c", "1")),
                is(false)
        );
    }

    @Test
    public void givenMatching__whenGroupAndArtifactSetted_andArtifactAndVersionMatch_andGroupDiffers__thenDoesntMatch() throws Exception {
        assertThat(
                new ArtifactCoordinates("a", "b", "1").matches(new ArtifactCoordinates("c", "b", "1")),
                is(false)
        );
    }

    @Test
    public void givenMatching__whenGroupIsNull_andArtifactAndVersionMatch_andGroupMatches__thenMatches() throws Exception {
        assertThat(
                new ArtifactCoordinates(null, "b", "1").matches(new ArtifactCoordinates(null, "b", "1")),
                is(true)
        );
    }

    @Test
    public void givenMatching__whenGroupIsNull_andArtifactAndVersionMatch_andGroupDiffers__thenDoesntMatch() throws Exception {
        assertThat(
                new ArtifactCoordinates(null, "b", "1").matches(new ArtifactCoordinates("a", "b", "1")),
                is(false)
        );
    }

    @Test
    public void givenMatching__whenArtifactIsNull_andGroupAndVersionMatch_andArtifactDiffers__thenDoesntMatch() throws Exception {
        assertThat(
                new ArtifactCoordinates("a", null, "1").matches(new ArtifactCoordinates("a", "b", "1")),
                is(false)
        );
    }

    @Test
    public void givenMatching__whenArtifactIsNull_andGroupAndVersionMatch_andArtifactMatch__thenMatch() throws Exception {
        assertThat(
                new ArtifactCoordinates("a", null, "1").matches(new ArtifactCoordinates("a", null, "1")),
                is(true)
        );
    }
}