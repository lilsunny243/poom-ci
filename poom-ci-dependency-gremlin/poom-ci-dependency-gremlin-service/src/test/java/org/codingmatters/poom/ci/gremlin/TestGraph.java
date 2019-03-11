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








        Vertex repo10 = g.addV("repository")
                .property("repository-id", "orga-repo10-branch")
                .property("name", "orga/repo10")
                .property("checkout-spec", "git|git@github.com:orga/repo10.git|branch")
                .next();
        Vertex repo11 = g.addV("repository")
                .property("repository-id", "orga-repo11-branch")
                .property("name", "orga/repo11")
                .property("checkout-spec", "git|git@github.com:orga/repo11.git|branch")
                .next();
        Vertex repo12 = g.addV("repository")
                .property("repository-id", "orga-repo12-branch")
                .property("name", "orga/repo12")
                .property("checkout-spec", "git|git@github.com:orga/repo12.git|branch")
                .next();
        Vertex repo13 = g.addV("repository")
                .property("repository-id", "orga-repo13-branch")
                .property("name", "orga/repo13")
                .property("checkout-spec", "git|git@github.com:orga/repo13.git|branch")
                .next();

        Vertex module10 = g.addV("module")
                .property("spec", "group:module10")
                .property("version", "1")
                .next();
        Vertex module11 = g.addV("module")
                .property("spec", "group:module11")
                .property("version", "1")
                .next();
        Vertex module12 = g.addV("module")
                .property("spec", "group:module12")
                .property("version", "1")
                .next();

        g.V(repo10).addE("produces").to(module10).next();
        g.V(repo11).addE("produces").to(module11).next();
        g.V(repo12).addE("produces").to(module12).next();


        g.V(repo11).addE("depends-on").to(module10).next();
        g.V(repo12).addE("depends-on").to(module11).next();
        g.V(repo13).addE("depends-on").to(module12).next();
    }
}
