package org.codingmatters.poom.ci.gremlin.service.handlers;

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.codingmatters.poom.ci.dependency.api.RepositoryPutRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryPutResponse;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.api.types.ValueList;
import org.codingmatters.poom.ci.gremlin.queries.*;
import org.codingmatters.poom.ci.gremlin.service.Mappers;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class CreateOrUpdateRepository implements Function<RepositoryPutRequest, RepositoryPutResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(CreateOrUpdateRepository.class);

    private DriverRemoteConnection connection;

    public CreateOrUpdateRepository(DriverRemoteConnection connection) {
        this.connection = connection;
    }

    @Override
    public RepositoryPutResponse apply(RepositoryPutRequest request) {
        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(this.connection);

        new CreateOrUpdateRepositoryQuery(g).update(request.repositoryId(), request.payload().name(), request.payload().checkoutSpec());
        new UpdateRepositoryProducedByQuery(g).update(request.repositoryId(), this.moduleSpecs(request.payload().produces()));
        new UpdateRepositoryDependenciesQuery(g).update(request.repositoryId(), this.moduleSpecs(request.payload().dependencies()));

        Optional<Repository> repository = new RepositoryQuery<>(AnonymousTraversalSource.traversal().withRemote(this.connection), Mappers::repository)
                .repository(request.repositoryId());

        log.info("updated repository with : {}", request.payload());
        return RepositoryPutResponse.builder().status200(status -> status.payload(repository.get())).build();
    }

    private Schema.ModuleSpec[] moduleSpecs(ValueList<Module> produces) {
        List<Schema.ModuleSpec> result = new LinkedList<>();
        produces.stream().forEach(module -> result.add(new Schema.ModuleSpec(module.spec(), module.version())));
        return result.toArray(new Schema.ModuleSpec[result.size()]);
    }
}
