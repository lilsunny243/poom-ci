package org.codingmatters.poom.ci.apps.releaser.maven;

import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class PomTest {

    @Test
    public void whenReadingProjectArtifactCoordinates__thenCoordinatesAreRead() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-pom.xml"));

        assertThat(pom.project().getGroupId(), is("org.codingmatters.test"));
        assertThat(pom.project().getArtifactId(), is("test"));
        assertThat(pom.project().getVersion(), is("1.0.0-SNAPSHOT"));
    }

    @Test
    public void whenChangingProjectVersion__thenResultingPomHasVersionChanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-pom.xml"));

        Pom.PomSource changedPomSource = pom.withVersion("2.0.0-SNAPSHOT");
        Pom changedPom = new Pom(changedPomSource);

        assertThat(changedPom.project().getGroupId(), is("org.codingmatters.test"));
        assertThat(changedPom.project().getArtifactId(), is("test"));
        assertThat(changedPom.project().getVersion(), is("2.0.0-SNAPSHOT"));
    }

    @Test
    public void whenChangingProjectVersionWithSameVersion__thenResultingPomIsUnchanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-pom.xml"));
        Pom.PomSource samePomSource = pom.withVersion("1.0.0-SNAPSHOT");

        assertThat(content(samePomSource.reader()), is(content("poms/snapshot-pom.xml")));
    }

    @Test
    public void givenPomWithParent__whenGettingParentCoordinates__thenCoordinatesAreRead() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-parent-pom.xml"));

        assertThat(pom.parent().getGroupId(), is("org.codingmatters.test"));
        assertThat(pom.parent().getArtifactId(), is("test-parent"));
        assertThat(pom.parent().getVersion(), is("1.2.3-SNAPSHOT"));
    }

    @Test
    public void givenPomWithParent__whenChangingParentVersion__ResultingPomHasVersionChanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-parent-pom.xml"));

        Pom.PomSource changedPomSource = pom.withParentVersion("1.2.3");
        Pom changedPom = new Pom(changedPomSource);

        assertThat(changedPom.parent().getGroupId(), is("org.codingmatters.test"));
        assertThat(changedPom.parent().getArtifactId(), is("test-parent"));
        assertThat(changedPom.parent().getVersion(), is("1.2.3"));
    }

    @Test
    public void givenPomWithOneDependency__whenReadingDependencyVersion__thenVersionRead() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-dep-pom.xml"));

        assertThat(pom.dependencyVersion("org.codingmatters.test", "test-dep").get(), is("1.2.3-SNAPSHOT"));
    }

    @Test
    public void givenPomWithOneDependency__whenReadingDependencyVersion_andDependencyDoesntExist__thenVersionNotPresent() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-dep-pom.xml"));

        assertThat(pom.dependencyVersion("no.such", "dep").isPresent(), is(false));
    }

    @Test
    public void givenPomWithManyDependency__whenReadingDependencyVersion__thenVersionRead() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/many-deps-pom.xml"));

        assertThat(pom.dependencyVersion("org.codingmatters.test", "test-dep-1").get(), is("1.2.3-SNAPSHOT"));
    }

    @Test
    public void givenPomWithManyDependency__whenReadingDependencyVersion_andDependencyDoesntExist__thenVersionNotPresent() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/many-deps-pom.xml"));

        Optional<String> depVersion = pom.dependencyVersion("no.such", "dep");

        assertThat(depVersion.isPresent(), is(false));
    }

    @Test
    public void givenPomWithManyDependency__whenChangingDependencVersion__thenResultingPomHasVersionChanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-many-deps-pom.xml"));

        Pom.PomSource changedPomSource = pom.withDependencyVersion("org.codingmatters.test", "test-dep-1", "1.2.3");
        Pom changedPom = new Pom(changedPomSource);

        assertThat(changedPom.dependencyVersion("org.codingmatters.test", "test-dep-1").get(), is("1.2.3"));
    }

    @Test
    public void givenPomWithManyDependency__whenChangingProjectVersionWithSameVersion__thenResultingPomIsUnchanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-many-deps-pom.xml"));
        Pom.PomSource samePomSource = pom.withDependencyVersion("org.codingmatters.test", "test-dep-1", "1.2.3-SNAPSHOT");

        assertThat(content(samePomSource.reader()), is(content("poms/snapshot-many-deps-pom.xml")));
    }





    @Test
    public void givenPomWithDependencyManagement__whenReadingDependencyVersion_andDepOnlyInDepMgmt__thenVersionRead() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-dep-mgmt-pom.xml"));

        assertThat(pom.dependencyVersion("org.codingmatters.test", "test-dep-1").get(), is("1.2.3-SNAPSHOT"));
    }

    @Test
    public void givenPomWithDependencyManagement__whenReadingDependencyVersion_andDepVersionInDepMgmt__thenVersionRead() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-dep-mgmt-pom.xml"));

        assertThat(pom.dependencyVersion("org.codingmatters.test", "test-dep-2").get(), is("4.5.6-SNAPSHOT"));
    }

    @Test
    public void givenPomWithDependencyManagement__whenReadingDependencyVersion_andDependencyDoesntExist__thenVersionNotPresent() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-dep-mgmt-pom.xml"));

        assertThat(pom.dependencyVersion("no.such", "dep").isPresent(), is(false));
    }

    @Test
    public void givenPomWithDependencyManagement__whenChangingDependencVersion__thenResultingPomHasVersionChanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-dep-mgmt-pom.xml"));

        Pom.PomSource changedPomSource = pom.withDependencyVersion("org.codingmatters.test", "test-dep-1", "1.2.3");
        assertThat(new Pom(changedPomSource).dependencyVersion("org.codingmatters.test", "test-dep-1").get(), is("1.2.3"));

        changedPomSource = new Pom(changedPomSource).withDependencyVersion("org.codingmatters.test", "test-dep-2", "4.5.6");
        assertThat(new Pom(changedPomSource).dependencyVersion("org.codingmatters.test", "test-dep-2").get(), is("4.5.6"));
    }

    @Test
    public void givenPomWithDependencyManagement__whenChangingProjectVersionWithSameVersion__thenResultingPomIsUnchanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-dep-mgmt-pom.xml"));
        Pom.PomSource samePomSource = pom.withDependencyVersion("org.codingmatters.test", "test-dep-1", "1.2.3-SNAPSHOT");

        assertThat(content(samePomSource.reader()), is(content("poms/snapshot-dep-mgmt-pom.xml")));

        samePomSource = pom.withDependencyVersion("org.codingmatters.test", "test-dep-2", "4.5.6-SNAPSHOT");
        assertThat(content(samePomSource.reader()), is(content("poms/snapshot-dep-mgmt-pom.xml")));
    }




    @Test
    public void givenPomWithProperties__whenReadingProperty_andPropertyExists__thenPropertyRead() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-dep-property-pom.xml"));

        assertThat(pom.property("test-dep.version").get(), is("1.2.3-SNAPSHOT"));
    }

    @Test
    public void givenPomWithProperties__whenReadingProperty_andPropertyDoesntExists__thenPropertyRead() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-dep-property-pom.xml"));

        assertThat(pom.property("not.found").isPresent(), is(false));
    }

    @Test
    public void givenPomWithDependencyProperties__whenReadingDependencyVersion__thenVersionReadWithResolvedProperty() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-dep-property-pom.xml"));

        assertThat(pom.dependencyVersion("org.codingmatters.test", "test-dep").get(), is("1.2.3-SNAPSHOT"));
    }

    @Test
    public void givenPomWithDependencyManagementProperties__whenReadingDependencyVersion__thenVersionReadWithResolvedProperty() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-dep-mgmt-property-pom.xml"));

        assertThat(pom.dependencyVersion("org.codingmatters.test", "test-dep").get(), is("1.2.3-SNAPSHOT"));
    }


    @Test
    public void givenPomWithDependencyProperties__whenChangingDependencyVersion__thenResultingPomHasVersionChanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-dep-property-pom.xml"));

        Pom.PomSource changedPomSource = pom.withDependencyVersion("org.codingmatters.test", "test-dep", "1.2.3");

        assertThat(new Pom(changedPomSource).dependencyVersion("org.codingmatters.test", "test-dep").get(), is("1.2.3"));
        assertThat(new Pom(changedPomSource).property("test-dep.version").get(), is("1.2.3"));
    }

    @Test
    public void givenPomWithDependencyManagementProperties__whenChangingDependencVersion__thenResultingPomHasVersionChanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-dep-mgmt-property-pom.xml"));

        Pom.PomSource changedPomSource = pom.withDependencyVersion("org.codingmatters.test", "test-dep", "1.2.3");
        try(Reader reader = changedPomSource.reader()) {
            char[] buffer = new char[1024];
            StringBuilder changedPom = new StringBuilder();
            for(int read = reader.read(buffer); read != -1 ; read = reader.read(buffer)) {
                changedPom.append(buffer, 0, read);
            }
            System.out.println("#######################################");
            System.out.println(changedPom.toString());
            System.out.println("#######################################");
        }

        assertThat(new Pom(changedPomSource).dependencyVersion("org.codingmatters.test", "test-dep").get(), is("1.2.3"));
        assertThat(new Pom(changedPomSource).property("test-dep.version").get(), is("1.2.3"));
    }



    @Test
    public void givenPomWithPlugin__whenReadingPluginVersion__thenPluginVersionRead() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-plugin-pom.xml"));

        assertThat(pom.pluginVersion("org.codingmatters.test", "test-plugin").get(), is("1.2.3-SNAPSHOT"));
    }

    @Test
    public void givenPomWithPlugin__whenChangingPluginWithSameVersion__thenResultingPomIsUnchanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-plugin-pom.xml"));

        Pom.PomSource changedPomSource = pom.withPluginVersion("org.codingmatters.test", "test-plugin", "1.2.3-SNAPSHOT");

        assertThat(this.content(changedPomSource.reader()), is(this.content("poms/snapshot-plugin-pom.xml")));
    }

    @Test
    public void givenPomWithPlugin__whenChangingPluginWithNewVersion__thenResultingPomHasPluginVersionChanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-plugin-pom.xml"));

        Pom.PomSource changedPomSource = pom.withPluginVersion("org.codingmatters.test", "test-plugin", "1.2.3");

        assertThat(new Pom(changedPomSource).pluginVersion("org.codingmatters.test", "test-plugin").get(), is("1.2.3"));
    }




    @Test
    public void givenPomWithPlugin__whenReadingPluginVersion_andVersionInPoperty__thenPluginVersionRead() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-plugin-properyt-pom.xml"));

        assertThat(pom.pluginVersion("org.codingmatters.test", "test-plugin").get(), is("1.2.3-SNAPSHOT"));
    }

    @Test
    public void givenPomWithPlugin__whenChangingPluginWithSameVersion_andVersionInPoperty__thenResultingPomIsUnchanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-plugin-properyt-pom.xml"));

        Pom.PomSource changedPomSource = pom.withPluginVersion("org.codingmatters.test", "test-plugin", "1.2.3-SNAPSHOT");

        assertThat(this.content(changedPomSource.reader()), is(this.content("poms/snapshot-plugin-properyt-pom.xml")));
    }

    @Test
    public void givenPomWithPlugin__whenChangingPluginWithNewVersion_andVersionInPoperty__thenResultingPomHasPluginVersionChanged_andPropertyChanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-plugin-properyt-pom.xml"));

        Pom.PomSource changedPomSource = pom.withPluginVersion("org.codingmatters.test", "test-plugin", "1.2.3");

        assertThat(new Pom(changedPomSource).pluginVersion("org.codingmatters.test", "test-plugin").get(), is("1.2.3"));
        assertThat(new Pom(changedPomSource).property("test-plugin.version").get(), is("1.2.3"));
    }








    @Test
    public void givenPomWithPlugin__whenReadingPluginVersion_andVersionInPluginMgmt__thenPluginVersionRead() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-plugin-mgmt-pom.xml"));

        assertThat(pom.pluginVersion("org.codingmatters.test", "test-plugin").get(), is("1.2.3-SNAPSHOT"));
    }

    @Test
    public void givenPomWithPlugin__whenChangingPluginWithSameVersion_andVersionInPluginMgmt__thenResultingPomIsUnchanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-plugin-mgmt-pom.xml"));

        Pom.PomSource changedPomSource = pom.withPluginVersion("org.codingmatters.test", "test-plugin", "1.2.3-SNAPSHOT");

        assertThat(this.content(changedPomSource.reader()), is(this.content("poms/snapshot-plugin-mgmt-pom.xml")));
    }

    @Test
    public void givenPomWithPlugin__whenChangingPluginWithNewVersion_andVersionInPluginMgmt__thenResultingPomHasPluginVersionChanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-plugin-mgmt-pom.xml"));

        Pom.PomSource changedPomSource = pom.withPluginVersion("org.codingmatters.test", "test-plugin", "1.2.3");

        assertThat(new Pom(changedPomSource).pluginVersion("org.codingmatters.test", "test-plugin").get(), is("1.2.3"));
    }









    @Test
    public void givenPomWithPlugin__whenReadingPluginVersion_andVersionInPluginMgmt_andVersionInProperty__thenPluginVersionRead() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-plugin-property-mgmt-pom.xml"));

        assertThat(pom.pluginVersion("org.codingmatters.test", "test-plugin").get(), is("1.2.3-SNAPSHOT"));
    }

    @Test
    public void givenPomWithPlugin__whenChangingPluginWithSameVersion_andVersionInPluginMgmt_andVersionInProperty__thenResultingPomIsUnchanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-plugin-property-mgmt-pom.xml"));

        Pom.PomSource changedPomSource = pom.withPluginVersion("org.codingmatters.test", "test-plugin", "1.2.3-SNAPSHOT");

        assertThat(this.content(changedPomSource.reader()), is(this.content("poms/snapshot-plugin-property-mgmt-pom.xml")));
    }

    @Test
    public void givenPomWithPlugin__whenChangingPluginWithNewVersion_andVersionInPluginMgmt_andVersionInProperty__thenResultingPomHasPluginVersionChanged_andPropertyChanged() throws Exception {
        Pom pom = Pom.from(Thread.currentThread().getContextClassLoader().getResourceAsStream("poms/snapshot-plugin-property-mgmt-pom.xml"));

        Pom.PomSource changedPomSource = pom.withPluginVersion("org.codingmatters.test", "test-plugin", "1.2.3");

        assertThat(new Pom(changedPomSource).pluginVersion("org.codingmatters.test", "test-plugin").get(), is("1.2.3"));
        assertThat(new Pom(changedPomSource).property("test-plugin.version").get(), is("1.2.3"));
    }











    private String content(String resource) throws IOException {
        try(Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))) {
            return content(reader);
        }
    }


    private String content(Reader reader) throws IOException {
        StringBuilder result = new StringBuilder();
        char[] buffer = new char[1024];
        for(int read = reader.read(buffer); read != -1; read = reader.read()) {
            result.append(buffer, 0, read);
        }
        return result.toString();
    }
}