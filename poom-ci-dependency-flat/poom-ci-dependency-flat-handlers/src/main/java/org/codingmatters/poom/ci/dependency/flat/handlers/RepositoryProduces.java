package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryModulesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryModulesGetResponse;
import org.codingmatters.poom.ci.dependency.api.repositorymodulesgetresponse.Status200;
import org.codingmatters.poom.ci.dependency.api.repositorymodulesgetresponse.Status404;
import org.codingmatters.poom.ci.dependency.api.repositorymodulesgetresponse.Status500;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;
import org.codingmatters.poom.ci.dependency.flat.GraphManagerException;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.Optional;
import java.util.function.Function;

public class RepositoryProduces implements Function<RepositoryModulesGetRequest, RepositoryModulesGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RepositoryProduces.class);
    private final GraphManager graphManager;

    public RepositoryProduces(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    @Override
    public RepositoryModulesGetResponse apply(RepositoryModulesGetRequest request) {
        Optional<Repository> repository = null;
        try {
            repository = this.graphManager.repository(request.repositoryId());
        } catch (GraphManagerException e) {
            return RepositoryModulesGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(log.tokenized().error("error getting repository " + request, e))
                    .build()).build()).build();
        }
        if(repository.isPresent()) {
            try {
                return RepositoryModulesGetResponse.builder().status200(Status200.builder()
                        .payload(this.graphManager.producedBy(repository.get()))
                        .build()).build();
            } catch (GraphManagerException e) {
                return RepositoryModulesGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                        .code(Error.Code.UNEXPECTED_ERROR)
                        .token(log.tokenized().error("error getting dependencies " + request, e))
                        .build()).build()).build();
            }
        } else {
            return RepositoryModulesGetResponse.builder().status404(Status404.builder().payload(Error.builder()
                    .code(Error.Code.RESOURCE_NOT_FOUND)
                    .token(log.tokenized().info("no such repository {}", request))
                    .description("no such repository")
                    .build()).build()).build();
        }
    }
}
