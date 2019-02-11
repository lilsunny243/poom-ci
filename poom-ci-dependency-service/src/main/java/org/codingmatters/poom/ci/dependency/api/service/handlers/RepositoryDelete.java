package org.codingmatters.poom.ci.dependency.api.service.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryDeleteRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryDeleteResponse;
import org.codingmatters.poom.ci.dependency.api.repositorydeleteresponse.Status200;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.graph.DependencyGraph;
import org.codingmatters.poom.ci.dependency.graph.tinkerpop.TinkerPopDependencyGraph;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

public class RepositoryDelete implements Function<RepositoryDeleteRequest, RepositoryDeleteResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RepositoryDelete.class);

    private final DependencyGraph dependencyGraph;

    public RepositoryDelete(DependencyGraph dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }

    @Override
    public RepositoryDeleteResponse apply(RepositoryDeleteRequest request) {
        Optional<Repository> repo = this.dependencyGraph.repositoryById(request.repositoryId());
        if(repo.isPresent()) {
            try {
                this.dependencyGraph.remove(repo.get());
                return RepositoryDeleteResponse.builder().status200(Status200.builder().build()).build();
            } catch (IOException e) {
                return RepositoryDeleteResponse.builder()
                        .status500(status -> status.payload(error -> error
                                .code(Error.Code.UNEXPECTED_ERROR)
                                .token(log.tokenized().error("error deleting repository " + repo.get(), e))
                        ))
                        .build();
            }
        } else {
            return RepositoryDeleteResponse.builder()
                    .status404(status -> status.payload(error -> error.code(Error.Code.RESOURCE_NOT_FOUND)))
                    .build();
        }
    }
}
