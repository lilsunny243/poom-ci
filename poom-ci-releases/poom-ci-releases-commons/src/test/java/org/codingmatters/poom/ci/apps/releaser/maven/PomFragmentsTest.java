package org.codingmatters.poom.ci.apps.releaser.maven;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class PomFragmentsTest {
    @Test
    public void givenPomWithDeps__whenReplacingDependency__theReplaced() throws Exception {
        String resource = "poms/snapshot-many-deps-pom.xml";
        PomFragments fragments = new PomFragments(this.sourceFromResource(resource));

        Pom.PomSource replacedSource = fragments.replaceDependency("org.codingmatters.test", "test-dep-2", "4.5.6-SNAPSHOT")
                .with("G", "A", "V");

        this.print(replacedSource);

        Pom replaced = new Pom(replacedSource);

        assertThat(replaced.dependencyVersion("org.codingmatters.test", "test-dep-2").isPresent(), is(false));
        assertThat(replaced.dependencyVersion("G", "A").get(), is("V"));
    }

    @Test
    public void givenPomWithDependencyManagement__whenReplacingDependencyInDependencyManagement__thenReplaced() throws Exception {
        String resource = "poms/snapshot-dep-mgmt-pom.xml";
        PomFragments fragments = new PomFragments(this.sourceFromResource(resource));

        Pom.PomSource replacedSource = fragments.replaceDependencyInManagement("org.codingmatters.test", "test-dep-2", "4.5.6-SNAPSHOT")
                .with("G", "A", "V");

        this.print(replacedSource);

        Pom replaced = new Pom(replacedSource);

        assertThat(replaced.dependencyVersion("org.codingmatters.test", "test-dep-2").isPresent(), is(false));
        assertThat(replaced.dependencyVersion("G", "A").get(), is("V"));
    }



    @Test
    public void givenPomWithPlugins__whenReplacingPlugin__thenReplaced() throws Exception {
        String resource = "poms/snapshot-plugin-pom.xml";
        PomFragments fragments = new PomFragments(this.sourceFromResource(resource));

        Pom.PomSource replacedSource = fragments.replacePlugin("org.codingmatters.test", "test-plugin", "1.2.3-SNAPSHOT")
                .with("G", "A", "V");

        this.print(replacedSource);

        Pom replaced = new Pom(replacedSource);

        assertThat(replaced.pluginVersion("org.codingmatters.test", "test-dep-2").isPresent(), is(false));
        assertThat(replaced.pluginVersion("G", "A").get(), is("V"));
    }

    @Test
    public void givenPomWithPlugins__whenReplacingPluginInPluginManagement__thenReplaced() throws Exception {
        String resource = "poms/snapshot-plugin-mgmt-pom.xml";
        PomFragments fragments = new PomFragments(this.sourceFromResource(resource));

        Pom.PomSource replacedSource = fragments.replacePluginInManagement("org.codingmatters.test", "test-plugin", "1.2.3-SNAPSHOT")
                .with("G", "A", "V");

        this.print(replacedSource);

        Pom replaced = new Pom(replacedSource);

        assertThat(replaced.pluginVersion("org.codingmatters.test", "test-dep-2").isPresent(), is(false));
        assertThat(replaced.pluginVersion("G", "A").get(), is("V"));
    }

    @Test
    public void givenPomWithProperty__whenReplacingProperty__thenReplaced() throws Exception {
        String resource = "poms/snapshot-property.xml";
        PomFragments fragments = new PomFragments(this.sourceFromResource(resource));

        Pom.PomSource replacedSource = fragments.replaceProperty("test-plugin.version", "1.2.3-SNAPSHOT")
                .with("V");

        this.print(replacedSource);

        Pom replaced = new Pom(replacedSource);

        assertThat(replaced.property("test-plugin.version").get(), is("V"));
        assertThat(replaced.property("test-plugin-2.version").get(), is("4.5.6-SNAPSHOT"));
    }

    @Test
    public void givenPomWithParent__whenReplacingParent__thenReplaced() throws Exception {
        String resource = "poms/snapshot-parent-pom.xml";
        PomFragments fragments = new PomFragments(this.sourceFromResource(resource));

        Pom.PomSource replacedSource = fragments.replaceParent("org.codingmatters.test", "test-parent", "1.2.3-SNAPSHOT")
                .with("G", "A", "V");

        this.print(replacedSource);

        Pom replaced = new Pom(replacedSource);

        assertThat(replaced.parent().getGroupId(), is("G"));
        assertThat(replaced.parent().getArtifactId(), is("A"));
        assertThat(replaced.parent().getVersion(), is("V"));
    }

    @Test
    public void givenInversedPom__whenProjectCoordinates__thenReplaced() throws Exception {
        String resource = "poms/inversed-pom.xml";
        PomFragments fragments = new PomFragments(this.sourceFromResource(resource));

        Pom.PomSource replacedSource = fragments.replaceProject("org.codingmatters.test", "test", "1.2.3-SNAPSHOT")
                .with("G", "A", "V");

        this.print(replacedSource);

        Pom replaced = new Pom(replacedSource);

        assertThat(replaced.project().getGroupId(), is("G"));
        assertThat(replaced.project().getArtifactId(), is("A"));
        assertThat(replaced.project().getVersion(), is("V"));
    }






    private Pom.PomSource sourceFromResource(String resource) {
        return () -> new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource));
    }

    private void print(Pom.PomSource source) throws IOException {
        try(Reader reader = source.reader()) {
            char[] buffer = new char[1024];
            for(int read = reader.read(buffer); read != -1; read = reader.read(buffer)) {
                System.out.print(new String(buffer, 0, read));
            }
        }
        System.out.println();
    }
}