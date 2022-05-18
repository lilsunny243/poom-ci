package org.codingmatters.poom.ci.apps.releaser;

import org.codingmatters.poom.ci.apps.releaser.maven.Pom;
import org.codingmatters.poom.ci.apps.releaser.maven.pom.ArtifactCoordinates;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public interface ProjectDescriptor {
    public String defaultFilename();
    ArtifactCoordinates project();

    void writeTo(Writer writer) throws IOException;

    ProjectDescriptor changeVersion(ArtifactCoordinates artifact) throws IOException;

    boolean changedFrom(ProjectDescriptor pom) throws IOException;

    Reader reader() throws IOException;

}
