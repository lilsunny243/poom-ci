package org.codingmatters.poom.ci.apps.releaser.maven.fragments;

import org.codingmatters.poom.ci.apps.releaser.maven.Pom;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplaceProperty {
    static private Pattern PROPERTIES_LOCATOR_PATTERN = Pattern.compile("<properties>(.*?)</properties>", Pattern.DOTALL);

    private final String name;
    private final String value;
    private final String content;

    public ReplaceProperty(String name, String value, String content) {
        this.name = name;
        this.value = value;
        this.content = content;
    }

    public Pom.PomSource with(String withValue) {
        StringBuilder result = new StringBuilder();

        Matcher propertiesMatcher = PROPERTIES_LOCATOR_PATTERN.matcher(this.content);
        if(propertiesMatcher.find()) {
            result.append(this.content.substring(0, propertiesMatcher.start()));
            result.append(this.replace(this.content.substring(propertiesMatcher.start(), propertiesMatcher.end()), withValue));
            result.append(this.content.substring(propertiesMatcher.end()));
        } else {
            result.append(this.content);
        }

        return new Pom.InMemoryPomSource(result.toString());
    }

    private String replace(String properties, String withValue) {
        StringBuilder result = new StringBuilder();
        Matcher propertyMatcher = Pattern.compile(
                String.format("(\\s*<%s>\\s*)%s(\\s*</%s>\\s*)",
                    this.name, this.value, this.name
                ),
                Pattern.DOTALL
        ).matcher(properties);

        if(propertyMatcher.find()) {
            result.append(properties.substring(0, propertyMatcher.start()));
            result.append(propertyMatcher.group(1));
            result.append(withValue);
            result.append(propertyMatcher.group(2));
            result.append(properties.substring(propertyMatcher.end()));
        } else {
            result.append(properties);
        }

        return result.toString();
    }
}
