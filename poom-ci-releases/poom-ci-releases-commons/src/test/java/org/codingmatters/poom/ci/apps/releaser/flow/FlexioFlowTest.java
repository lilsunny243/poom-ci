package org.codingmatters.poom.ci.apps.releaser.flow;

import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

@Ignore
public class FlexioFlowTest {


    @Rule
    public TemporaryFolder dir = new TemporaryFolder();
    private File repository;

    @Before
    public void setUp() throws Exception {
        this.repository = this.dir.newFolder();
        try(
                InputStreamReader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("flexio-flow.yml"));
                FileWriter writer = new FileWriter(new File(this.repository, "flexio-flow.yml"))
        ) {
            char[] buffer = new char[1024];
            for(int read = reader.read(buffer) ; read != -1 ; read = reader.read(buffer)) {
                writer.write(buffer, 0, read);
            }
            writer.flush();
        }
    }

    @Test
    public void givenInFlowRepository__whenGettingVersion__thenVersionGettedFromYaml() throws Exception {
        FlexioFlow flow = new FlexioFlow(this.repository, new CommandHelper(line -> System.out.println(line), line -> System.err.println(line)));

        MatcherAssert.assertThat(flow.version(), Matchers.is("2.8.0-SNAPSHOT"));
    }
}