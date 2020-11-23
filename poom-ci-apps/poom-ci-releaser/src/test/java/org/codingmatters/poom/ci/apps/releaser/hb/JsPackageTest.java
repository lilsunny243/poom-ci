package org.codingmatters.poom.ci.apps.releaser.hb;

import org.codingmatters.poom.ci.apps.releaser.ProjectDescriptor;
import org.codingmatters.poom.ci.apps.releaser.maven.pom.ArtifactCoordinates;
import org.junit.Test;

import java.io.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;


public class JsPackageTest {

    @Test
    public void defaultFilename() throws Exception {
        assertThat(
                this.readJsPackage("hb/minimal.json").defaultFilename(),
                is("package.json")
        );
    }
    @Test
    public void readPackage() throws Exception {
        assertThat(
                this.readJsPackage("hb/minimal.json").project(),
                is(new ArtifactCoordinates("@flexio-oss", "hotballoon", "0.4.2-dev"))
        );
    }

    @Test
    public void readDependencies() throws Exception {
        assertThat(
                this.readJsPackage("hb/minimal-with-deps.json").dependencies(),
                contains(
                        new ArtifactCoordinates("@flexio-oss", "js-style-bundle", "1.4.0-dev"),
                        new ArtifactCoordinates("@flexio-oss", "hotballoon", "0.7.0"),
                        new ArtifactCoordinates("@flexio-oss", "js-commons-bundle", "1.3.0")
                )
        );
    }

    @Test
    public void changePackageVersion() throws Exception {
        JsPackage jsPackage = this.readJsPackage("hb/minimal-with-deps.json");
        ProjectDescriptor actual = jsPackage.changeVersion(new ArtifactCoordinates("@flexio-corp", "js-hotballoon-parent", "42.12.18"));

        assertThat(actual.project().getVersion(), is("42.12.18"));
    }

    @Test
    public void changeDependencyVersion() throws Exception {
        JsPackage jsPackage = this.readJsPackage("hb/minimal-with-deps.json");

        ProjectDescriptor actual = jsPackage.changeVersion(new ArtifactCoordinates("@flexio-oss", "hotballoon", "42.12.18"));

        assertThat(((JsPackage)actual).dependencies().get(1).getVersion(), is("42.12.18"));
    }

    @Test
    public void whenArtifactInDevDeps__thenArtifactVersionNotChanged() throws Exception {
        JsPackage jsPackage = this.readJsPackage("hb/minimal-with-dev-deps.json");

        JsPackage actual = (JsPackage) jsPackage.changeVersion(new ArtifactCoordinates("@flexio-corp", "component-commons-bundle", "42.12.18"));

        assertThat(actual.devDependencies(), contains(new ArtifactCoordinates("@flexio-corp", "component-commons-bundle", "1.0.0")));
    }

    @Test
    public void whenArtifactInDeps_andDevDeps__thenArtifactVersionNotChanged() throws Exception {
        JsPackage jsPackage = this.readJsPackage("hb/minimal-with-dev-and-dev-deps.json");

        JsPackage actual = (JsPackage) jsPackage.changeVersion(new ArtifactCoordinates("@flexio-corp", "component-commons-bundle", "42.12.18"));

        assertThat(actual.dependencies(), contains(new ArtifactCoordinates("@flexio-corp", "component-commons-bundle", "42.12.18")));
        assertThat(actual.devDependencies(), contains(new ArtifactCoordinates("@flexio-corp", "component-commons-bundle", "1.0.0")));
    }

    private JsPackage readJsPackage(String resource) throws IOException {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        return JsPackage.read(stream);
    }
}