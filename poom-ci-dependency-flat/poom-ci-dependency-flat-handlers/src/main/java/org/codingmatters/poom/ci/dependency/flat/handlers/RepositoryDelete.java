package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryDeleteRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryDeleteResponse;
import org.codingmatters.poom.ci.dependency.api.repositorydeleteresponse.Status200;
import org.codingmatters.poom.ci.dependency.api.repositorydeleteresponse.Status404;
import org.codingmatters.poom.ci.dependency.api.repositorydeleteresponse.Status500;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;
import org.codingmatters.poom.ci.dependency.flat.GraphManagerException;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.Optional;
import java.util.function.Function;

public class RepositoryDelete implements Function<RepositoryDeleteRequest, RepositoryDeleteResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RepositoryDelete.class);
    private final GraphManager graphManager;

    public RepositoryDelete(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    @Override
    public RepositoryDeleteResponse apply(RepositoryDeleteRequest request) {
        try {
            Optional<Repository> repository = this.graphManager.repository(request.repositoryId());
            if(repository.isPresent()) {
                this.graphManager.deleteRepository(request.repositoryId());
                log.info("deleted repository ", repository.get());
                return RepositoryDeleteResponse.builder().status200(Status200.builder().build()).build();
            } else {
                return RepositoryDeleteResponse.builder().status404(Status404.builder().payload(Error.builder()
                        .code(Error.Code.RESOURCE_NOT_FOUND)
                        .token(log.tokenized().info("repository not found for deletion : {}", request))
                        .description("no such repository")
                        .build()).build()).build();
            }
        } catch (GraphManagerException e) {
            return RepositoryDeleteResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(log.tokenized().error("unexpected error while deleting repository : " + request, e))
                    .build()).build()).build();
        }
    }
}
