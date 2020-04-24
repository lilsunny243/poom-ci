package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class UpdateRepositoryDependenciesQueryTest extends AbstractVertexQueryTest {


    @Test
    public void givenRepoHasNone__whenUpdatingWithNone__thenRepoHasNone() throws Exception {
        String repositoryId = "orga-repo5-branch";
        new UpdateRepositoryDependenciesQuery(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()))
                .update(repositoryId);

        List<Schema.ModuleSpec> deps = new DependenciesQuery<>(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()),
                map -> new Schema.ModuleSpec((String) map.get("spec").get(0).value(), (String) map.get("version").get(0).value())
        ).forRepository(repositoryId);

        assertThat(deps, is(empty()));
    }
    @Test
    public void givenRepoHasNone__whenUpdatingWithEmpty__thenRepoHasNone() throws Exception {
        String repositoryId = "orga-repo5-branch";
        new UpdateRepositoryDependenciesQuery(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()))
                .update(repositoryId, new Schema.ModuleSpec[0]);

        List<Schema.ModuleSpec> deps = new DependenciesQuery<>(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()),
                map -> new Schema.ModuleSpec((String) map.get("spec").get(0).value(), (String) map.get("version").get(0).value())
        ).forRepository(repositoryId);

        assertThat(deps, is(empty()));
    }

    @Test
    public void givenRepoHasNone__whenUpdatingWithOne__thenRepoHasThisOne() throws Exception {
        String repositoryId = "orga-repo5-branch";
        new UpdateRepositoryDependenciesQuery(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()))
                .update(repositoryId, new Schema.ModuleSpec("group:module", "1"));

        List<Schema.ModuleSpec> deps = new DependenciesQuery<>(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()),
                map -> new Schema.ModuleSpec((String) map.get("spec").get(0).value(), (String) map.get("version").get(0).value())
        ).forRepository(repositoryId);

        assertThat(deps, containsInAnyOrder(new Schema.ModuleSpec("group:module", "1")));
    }

    @Test
    public void givenRepoHasNone__whenUpdatingWithTwo__thenRepoHasThoseTwo() throws Exception {
        String repositoryId = "orga-repo5-branch";
        new UpdateRepositoryDependenciesQuery(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()))
                .update(repositoryId,
                        new Schema.ModuleSpec("group:module1", "1"),
                        new Schema.ModuleSpec("group:module2", "1")
                );

        List<Schema.ModuleSpec> deps = new DependenciesQuery<>(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()),
                map -> new Schema.ModuleSpec((String) map.get("spec").get(0).value(), (String) map.get("version").get(0).value())
        ).forRepository(repositoryId);

        assertThat(deps, containsInAnyOrder(
                new Schema.ModuleSpec("group:module1", "1"),
                new Schema.ModuleSpec("group:module2", "1")
        ));
    }

    @Test
    public void givenRepoHasOne__whenUpdatingWithOneMore__thenRepoHasThoseTwo() throws Exception {
        String repositoryId = "orga-repo13-branch";
        new UpdateRepositoryDependenciesQuery(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()))
                .update(repositoryId,
                        new Schema.ModuleSpec("group:module", "1"),
                        new Schema.ModuleSpec("group:module12", "1")
                );

        List<Schema.ModuleSpec> deps = new DependenciesQuery<>(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()),
                map -> new Schema.ModuleSpec((String) map.get("spec").get(0).value(), (String) map.get("version").get(0).value())
        ).forRepository(repositoryId);

        assertThat(deps, containsInAnyOrder(
                new Schema.ModuleSpec("group:module12", "1"),
                new Schema.ModuleSpec("group:module", "1")
        ));
    }
    @Test
    public void givenRepoHasOne__whenUpdatingWithAnotherOne__thenRepoHasLostFirstAndGainSecond() throws Exception {
        String repositoryId = "orga-repo13-branch";
        new UpdateRepositoryDependenciesQuery(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()))
                .update(repositoryId,
                        new Schema.ModuleSpec("group:module", "1")
                );

        List<Schema.ModuleSpec> deps = new DependenciesQuery<>(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()),
                map -> new Schema.ModuleSpec((String) map.get("spec").get(0).value(), (String) map.get("version").get(0).value())
        ).forRepository(repositoryId);

        assertThat(deps, containsInAnyOrder(
                new Schema.ModuleSpec("group:module", "1")
        ));
    }

    @Test
    public void givenRepoHasOne__whenUpdatingWithNone__thenRepoHasNone() throws Exception {
        String repositoryId = "orga-repo13-branch";
        new UpdateRepositoryDependenciesQuery(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()))
                .update(repositoryId);

        List<Schema.ModuleSpec> deps = new DependenciesQuery<>(AnonymousTraversalSource.traversal().withRemote(this.gremlin.remoteConnection()),
                map -> new Schema.ModuleSpec((String) map.get("spec").get(0).value(), (String) map.get("version").get(0).value())
        ).forRepository(repositoryId);

        assertThat(deps, is(empty()));
    }

}