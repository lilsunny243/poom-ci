package org.codingmatters.poom.ci.dependency.graph.tinkerpop;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;
import org.codingmatters.poom.ci.dependency.graph.DownstreamGraph;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*;

public class TinkerPopDownstreamGraph extends AbstractTinkerPopRepositoryGraph implements DownstreamGraph {

    static public TinkerPopDownstreamGraph from(DependencyGraph graph, Repository root) throws IOException {
        TinkerPopDownstreamGraph result = new TinkerPopDownstreamGraph(root);
        addDownstreams(graph, root, result);

        return result;
    }

    static private void addDownstreams(DependencyGraph graph, Repository root, TinkerPopDownstreamGraph result) throws IOException {
        List<Repository> toRecurse = new LinkedList<>();

        for (Repository repository : graph.downstream(root)) {
            boolean targetSeen = result.repositoryById(repository.id()).isPresent();

            if(! targetSeen) {
                result.add(repository);
                toRecurse.add(repository);
            }
            result.downstream(root, repository);
        }

        for (Repository repository : toRecurse) {
            addDownstreams(graph, repository, result);
        }

    }

    private TinkerPopDownstreamGraph(Repository root) throws IOException {
        super();
        this.add(root);
    }

    private DownstreamGraph downstream(Repository from, Repository to) {
        Vertex fromVertex = this.repositoryQuery(this.traversal(), from).next();
        Vertex toVertex = this.repositoryQuery(this.traversal(), to).next();

        GraphTraversal<Vertex, Edge> existingEdge = this.traversal().V(fromVertex.id()).bothE(DOWNSTREAM_PREDICATE).where(otherV().hasId(toVertex.id()));
        if(! existingEdge.hasNext()) {
            this.traversal().addE(DOWNSTREAM_PREDICATE).from(fromVertex).to(toVertex).next();
        }
        return this;
    }

    @Override
    public Repository[] direct(Repository from) {
        Set<Repository> results = new HashSet<>();

        GraphTraversal repositoryQuery = this.repositoryQuery(this.traversal(), from);
        if(repositoryQuery.hasNext()) {
            Vertex fromVertex = (Vertex) repositoryQuery.next();
            GraphTraversal<Vertex, Vertex>  sources = this.traversal().V(fromVertex.id()).out(DOWNSTREAM_PREDICATE);
            while(sources.hasNext()) {
                results.add(this.repositoryFrom(sources.next()));
            }
        }

        return results.toArray(new Repository[results.size()]);
    }

    @Override
    public Repository[] dependencyTreeFirstSteps(Repository root) {
        Repository[] direct = this.direct(root);
        if(direct.length == 0) return new Repository[0];

        Vertex rootVertex = (Vertex) this.repositoryQuery(this.traversal(), root).next();

        Set<String> secondLevelLinkedRepositoryId = new HashSet<>();
        GraphTraversal<Vertex, Vertex> targets = this.traversal().V(rootVertex)
                .repeat(out(DOWNSTREAM_PREDICATE))
                .until(cyclicPath())
                .emit(loops().is(P.gt(1)));
        while(targets.hasNext()) {
            secondLevelLinkedRepositoryId.add(targets.next().value("id"));
        }

        System.out.println(secondLevelLinkedRepositoryId);

        List<Repository> result = new LinkedList<>();
        for (Repository repository : direct) {
            if(! secondLevelLinkedRepositoryId.contains(repository.id())) {
                result.add(repository);
            }
        }

        return result.toArray(new Repository[result.size()]);
    }

    @Override
    protected void graphChanged() throws IOException {}
}
