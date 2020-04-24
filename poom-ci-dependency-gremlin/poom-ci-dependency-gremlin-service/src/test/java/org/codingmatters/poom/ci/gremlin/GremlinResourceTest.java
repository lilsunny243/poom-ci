package org.codingmatters.poom.ci.gremlin;

import io.flexio.docker.DockerResource;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class GremlinResourceTest {

    @ClassRule
    static public final DockerResource docker = GremlinResource.withGremlinContainer(DockerResource.client());

    @Rule
    public GremlinResource gremlin = new GremlinResource(docker);

    @Test
    public void graphIsInitiallyEmpty_1() throws Exception {
        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(gremlin.remoteConnection());

        assertThat(g.V().toList(), is(empty()));

        Vertex v1 = g.addV("person").property("name","marko").next();
        Vertex v2 = g.addV("person").property("name","stephen").next();
        g.V(v1).addE("knows").to(v2).property("weight",0.75).iterate();

        assertThat(g.V().toList(), is(not(empty())));
    }

    @Test
    public void graphIsInitiallyEmpty_2() throws Exception {
        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(gremlin.remoteConnection());

        assertThat(g.V().toList(), is(empty()));

        Vertex v1 = g.addV("person").property("name","marko").next();
        Vertex v2 = g.addV("person").property("name","stephen").next();
        g.V(v1).addE("knows").to(v2).property("weight",0.75).iterate();

        Vertex marko = g.V().has("person", "name", "marko").next();
        System.out.println(marko);

        assertThat(g.V().toList(), is(not(empty())));
    }

    @Test
    public void graphIsInitiallyEmpty_3() throws Exception {
        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(gremlin.remoteConnection());

        assertThat(g.V().toList(), is(empty()));

        Vertex v1 = g.addV("person").property("name","marko").next();
        Vertex v2 = g.addV("person").property("name","stephen").next();
        g.V(v1).addE("knows").to(v2).property("weight",0.75).iterate();

        assertThat(g.V().toList(), is(not(empty())));
    }
}