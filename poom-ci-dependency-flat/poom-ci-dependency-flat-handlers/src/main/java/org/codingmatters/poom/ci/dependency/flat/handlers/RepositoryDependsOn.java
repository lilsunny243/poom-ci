package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryDependenciesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryDependenciesGetResponse;
import org.codingmatters.poom.ci.dependency.api.repositorydependenciesgetresponse.Status200;
import org.codingmatters.poom.ci.dependency.api.repositorydependenciesgetresponse.Status404;
import org.codingmatters.poom.ci.dependency.api.repositorydependenciesgetresponse.Status500;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;
import org.codingmatters.poom.ci.dependency.flat.GraphManagerException;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.Optional;
import java.util.function.Function;

public class RepositoryDependsOn implements Function<RepositoryDependenciesGetRequest, RepositoryDependenciesGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RepositoryDependsOn.class);

    private final GraphManager graphManager;

    public RepositoryDependsOn(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    @Override
    public RepositoryDependenciesGetResponse apply(RepositoryDependenciesGetRequest request) {
        Optional<Repository> repository = null;
        try {
            repository = this.graphManager.repository(request.repositoryId());
        } catch (GraphManagerException e) {
            return RepositoryDependenciesGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(log.tokenized().error("error getting repository " + request, e))
                    .build()).build()).build();
        }
        if(repository.isPresent()) {
            try {
                return RepositoryDependenciesGetResponse.builder().status200(Status200.builder()
                        .payload(this.graphManager.dependenciesOf(repository.get()))
                        .build()).build();
            } catch (GraphManagerException e) {
                return RepositoryDependenciesGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                        .code(Error.Code.UNEXPECTED_ERROR)
                        .token(log.tokenized().error("error getting dependencies " + request, e))
                        .build()).build()).build();
            }
        } else {
            return RepositoryDependenciesGetResponse.builder().status404(Status404.builder().payload(Error.builder()
                    .code(Error.Code.RESOURCE_NOT_FOUND)
                    .token(log.tokenized().info("no such repository {}", request))
                    .description("no such repository")
                    .build()).build()).build();
        }
    }
}
