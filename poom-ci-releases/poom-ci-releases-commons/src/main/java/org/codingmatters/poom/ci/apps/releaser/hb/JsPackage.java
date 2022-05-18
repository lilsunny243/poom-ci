package org.codingmatters.poom.ci.apps.releaser.hb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.codingmatters.poom.ci.apps.releaser.ProjectDescriptor;
import org.codingmatters.poom.ci.apps.releaser.maven.pom.ArtifactCoordinates;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsPackage implements ProjectDescriptor {
    static private ObjectMapper MAPPER = new ObjectMapper();

    static public JsPackage read(InputStream in) throws IOException {
        StringBuilder content = new StringBuilder();
        try(Reader reader = new InputStreamReader(in)) {
            char[] buffer = new char[1024];
            for(int read = reader.read(buffer); read != -1 ; read = reader.read(buffer)) {
                content.append(buffer, 0, read);
            }
        }
        return new JsPackage(new InMemoryJsPackageSource(content.toString()));
    }

    private final JsPackageSource source;
    private ArtifactCoordinates projectCoordinates;
    private List<ArtifactCoordinates> dependencies = new LinkedList<>();
    private List<ArtifactCoordinates> devDependencies = new LinkedList<>();

    public JsPackage(JsPackageSource source) throws IOException {
        this.source = source;
        try(Reader reader = this.source.reader()) {
            Map<String, Object> asMap = MAPPER.readValue(reader, Map.class);
            this.projectCoordinates = this.coordinatesFromString((String) asMap.get("name"), (String) asMap.get("version"));

            if(asMap.get("dependencies") != null && asMap.get("dependencies") instanceof Map) {
                Map<String, String> depMap = (Map<String, String>) asMap.get("dependencies");
                depMap.forEach((name, version) -> this.dependencies.add(this.coordinatesFromString(name, version)));
            }
            if(asMap.get("devDependencies") != null && asMap.get("devDependencies") instanceof Map) {
                Map<String, String> depMap = (Map<String, String>) asMap.get("devDependencies");
                depMap.forEach((name, version) -> this.devDependencies.add(this.coordinatesFromString(name, version)));
            }
        }
    }

    private ArtifactCoordinates coordinatesFromString(String name, String version) {
        String groupId = null;
        String artifactId = null;
        if(name.contains("/")) {
            groupId = name.substring(0, name.indexOf('/'));
            artifactId = name.substring(name.indexOf('/') + 1);
        } else {
            artifactId = name;
        }
        return new ArtifactCoordinates(groupId, artifactId, version);
    }

    @Override
    public String defaultFilename() {
        return "package.json";
    }

    @Override
    public ArtifactCoordinates project() {
        return this.projectCoordinates;
    }

    public List<ArtifactCoordinates>dependencies() {
        return this.dependencies;
    }

    public List<ArtifactCoordinates> devDependencies() {
        return devDependencies;
    }

    @Override
    public void writeTo(Writer writer) throws IOException {
        try(Reader reader = this.source.reader()) {
            char[] buffer = new char[1024];
            for(int read = reader.read(buffer) ; read != -1 ; read = reader.read(buffer)) {
                writer.write(buffer, 0, read);
            }
        }
    }

    @Override
    public ProjectDescriptor changeVersion(ArtifactCoordinates artifact) throws IOException {
        JsPackageSource source = this.source;
        if(this.project().matches(artifact)) {
            source = this.changedProjectVersion(source, artifact);
        }

        if(! this.dependencies.isEmpty()) {
            for (ArtifactCoordinates dependency : this.dependencies) {
                if (dependency.matches(artifact)) {
                    source = this.changedDependencyVersion(source, artifact);
                }
            }
        }
        return new JsPackage(source);
    }

    static private Pattern PACKAGE_VERSION_PATTERN = Pattern.compile("(\"version\"\\s*:\\s*\")([^\"]*)\"");

    private JsPackageSource changedProjectVersion(JsPackageSource source, ArtifactCoordinates artifact) throws IOException {
        Matcher matcher = PACKAGE_VERSION_PATTERN.matcher(this.readContent(source));
        if(matcher.find()) {
            StringBuilder result = new StringBuilder();

            result.append(this.readContent(source), 0, matcher.start());
            result.append(matcher.group(1)).append(artifact.getVersion()).append("\"");
            result.append(this.readContent(source), matcher.end(), this.readContent(source).length());

            return new InMemoryJsPackageSource(result.toString());
        } else {
            return source;
        }
    }

    static private final Pattern DEPS_PATTERN = Pattern.compile("(\"dependencies\"\\s*:\\s*\\{)([^}]*)(\\})");

    private JsPackageSource changedDependencyVersion(JsPackageSource source, ArtifactCoordinates artifact) throws IOException {

        String content = this.readContent(source);
        Matcher depsMatcher = DEPS_PATTERN.matcher(content);
        if(depsMatcher.find()) {
            Pattern regex = Pattern.compile("(\"" + artifact.getGroupId() + "/" + artifact.getArtifactId() + "\"\\s*:\\s*\")([^\"]*)\"");
            Matcher matcher = regex.matcher(depsMatcher.group(2));

            if(matcher.find()) {
                StringBuilder result = new StringBuilder();

                result.append(depsMatcher.group(2), 0, matcher.start());
                result.append(matcher.group(1)).append(artifact.getVersion()).append("\"");
                result.append(depsMatcher.group(2), matcher.end(), depsMatcher.group(2).length());

                String changed =
                        content.substring(0, depsMatcher.start()) + depsMatcher.group(1) +
                        result.toString() +
                        depsMatcher.group(3) +
                        content.substring(depsMatcher.end())
                        ;

                return new InMemoryJsPackageSource(
                        changed
                );
            } else {
                return source;
            }
        } else {
            return source;
        }
    }

    private String readContent(JsPackageSource source) throws IOException {
        try(Reader reader = source.reader()) {
            return this.readString(reader);
        }
    }

    @NotNull
    private String readString(Reader reader) throws IOException {
        StringBuilder result = new StringBuilder();
        char[] buffer = new char[1024];
        for(int read = reader.read(buffer); read != -1 ; read = reader.read(buffer)) {
            result.append(buffer, 0, read);
        }
        return result.toString();
    }

    @Override
    public boolean changedFrom(ProjectDescriptor descriptor) throws IOException {
        return ! this.readString(this.reader()).equals(this.readString(descriptor.reader()));
    }

    @Override
    public Reader reader() throws IOException {
        return this.source.reader();
    }


    @FunctionalInterface
    public interface JsPackageSource {
        Reader reader() throws IOException;
    }

    static public class InMemoryJsPackageSource implements JsPackageSource {
        private final String content;

        public InMemoryJsPackageSource(String content) {
            this.content = content;
        }

        @Override
        public Reader reader() throws IOException {
            return new StringReader(this.content);
        }
    }
}
