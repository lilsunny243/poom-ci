package org.codingmatters.poom.ci.apps.releaser.maven;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PatternTest {

    @Test
    public void test1() throws Exception {
        Pattern pattern = Pattern.compile("<a>(.*?)</a>");

        Matcher m = pattern.matcher("123<a>A</a><a>B</a>");

        assertThat(m.find(), is(true));
        assertThat(m.group(1), is("A"));
    }

    @Test
    public void test2() throws Exception {
        String pom = this.content("poms/snapshot-many-deps-pom.xml");
        Pattern dependenciesPattern = Pattern.compile("<dependencies>(.*?)</dependencies>");
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
