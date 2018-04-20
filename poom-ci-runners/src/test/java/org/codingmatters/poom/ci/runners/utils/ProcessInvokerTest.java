package org.codingmatters.poom.ci.runners.utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ProcessInvokerTest {

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    @Test
    public void output() throws Exception {
        for (int i = 0; i < 10; i++) {
            this.dir.newFile();
        }
        List<String> output = Collections.synchronizedList(new LinkedList<>());

        int status = new ProcessInvoker().exec(new ProcessBuilder("ls", this.dir.getRoot().getAbsolutePath()),
                line -> output.add(line),
                null);

        assertThat(status, is(0));
        assertThat(output, hasSize(10));
    }

    @Test
    public void error() throws Exception {
        List<String> error = Collections.synchronizedList(new LinkedList<>());

        int status = new ProcessInvoker().exec(new ProcessBuilder("logger", "-s", "message"),
                null,
                line -> error.add(line));

        assertThat(error, hasSize(1));

        System.out.println(error);
    }
}