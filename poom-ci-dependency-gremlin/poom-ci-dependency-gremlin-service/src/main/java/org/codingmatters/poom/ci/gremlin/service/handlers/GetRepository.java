package org.codingmatters.poom.ci.gremlin.service.handlers;

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.codingmatters.poom.ci.dependency.api.RepositoriesGetResponse;
import org.codingmatters.poom.ci.dependency.api.RepositoryGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryGetResponse;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.gremlin.queries.RepositoryQuery;
import org.codingmatters.poom.ci.gremlin.service.Mappers;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class GetRepository implements Function<RepositoryGetRequest, RepositoryGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(GetRepository.class);
    private Supplier<RemoteConnection> connectionSupplier;

    public GetRepository(Supplier<RemoteConnection> connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    @Override
    public RepositoryGetResponse apply(RepositoryGetRequest request) {
        try(RemoteConnection connection = this.connectionSupplier.get()) {
            Optional<Repository> repository = new RepositoryQuery<>(AnonymousTraversalSource.traversal().withRemote(connection), Mappers::repository)
                    .repository(request.repositoryId());

            if(repository.isPresent()) {
                return RepositoryGetResponse.builder()
                        .status200(status -> status.payload(repository.get()))
                        .build();
            } else {
                return RepositoryGetResponse.builder()
                        .status404(status -> status.payload(error -> error
                                .code(Error.Code.RESOURCE_NOT_FOUND)
                                .token(log.tokenized().info("no such repository {}", request.repositoryId()))))
                        .build();
            }
        } catch (Exception e) {
            return RepositoryGetResponse.builder()
                    .status500(status -> status.payload(Error.builder().code(Error.Code.UNEXPECTED_ERROR).token(log.tokenized().error("error accessing gremlin server", e)).build()))
                    .build();
        }
    }
}
