package org.codingmatters.poom.ci.gremlin;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class TestGraph {
    static public void setup(GraphTraversalSource g) {
        Vertex repo1 = g.addV("repository")
                .property("repository-id", "orga-repo1-branch")
                .property("name", "orga/repo1")
                .property("checkout-spec", "git|git@github.com:orga/repo1.git|branch")
                .next();
        Vertex repo2 = g.addV("repository")
                .property("repository-id", "orga-repo2-branch")
                .property("name", "orga/repo2")
                .property("checkout-spec", "git|git@github.com:orga/repo2.git|branch")
                .next();
        Vertex repo3 = g.addV("repository")
                .property("repository-id", "orga-repo3-branch")
                .property("name", "orga/repo3")
                .property("checkout-spec", "git|git@github.com:orga/repo3.git|branch")
                .next();
        Vertex repo4 = g.addV("repository")
                .property("repository-id", "orga-repo4-branch")
                .property("name", "orga/repo4")
                .property("checkout-spec", "git|git@github.com:orga/repo4.git|branch")
                .next();
        Vertex repo5 = g.addV("repository")
                .property("repository-id", "orga-repo5-branch")
                .property("name", "orga/repo5")
                .property("checkout-spec", "git|git@github.com:orga/repo5.git|branch")
                .next();

        Vertex module1 = g.addV("module")
                .property("spec", "group:module1")
                .property("version", "1")
                .next();
        Vertex module2 = g.addV("module")
                .property("spec", "group:module2")
                .property("version", "1")
                .next();
        Vertex module3 = g.addV("module")
                .property("spec", "group:module3")
                .property("version", "1")
                .next();
        Vertex module4 = g.addV("module")
                .property("spec", "group:module4")
                .property("version", "1")
                .next();
        Vertex module5 = g.addV("module")
                .property("spec", "group:module5")
                .property("version", "1")
                .next();
        Vertex externalModule = g.addV("module")
                .property("spec", "external:dep")
                .property("version", "1")
                .next();

        g.V(repo1).addE("produces").to(module1).next();
        g.V(repo1).addE("produces").to(module2).next();
        g.V(repo2).addE("produces").to(module3).next();
        g.V(repo4).addE("produces").to(module5).next();
        g.V(repo5).addE("produces").to(module4).next();

        g.V(repo1).addE("depends-on").to(externalModule).next();
        g.V(repo2).addE("depends-on").to(module2).next();
        g.V(repo3).addE("depends-on").to(module3).next();
        g.V(repo3).addE("depends-on").to(module4).next();
        g.V(repo4).addE("depends-on").to(module1).next();
        g.V(repo4).addE("depends-on").to(module3).next();
    }
}
