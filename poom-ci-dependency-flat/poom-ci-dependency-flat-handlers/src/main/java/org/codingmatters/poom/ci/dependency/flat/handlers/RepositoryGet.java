package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryGetResponse;
import org.codingmatters.poom.ci.dependency.api.repositorygetresponse.Status200;
import org.codingmatters.poom.ci.dependency.api.repositorygetresponse.Status400;
import org.codingmatters.poom.ci.dependency.api.repositorygetresponse.Status404;
import org.codingmatters.poom.ci.dependency.api.repositorygetresponse.Status500;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;
import org.codingmatters.poom.ci.dependency.flat.GraphManagerException;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.Optional;
import java.util.function.Function;

public class RepositoryGet implements Function<RepositoryGetRequest, RepositoryGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RepositoryGet.class);
    private final GraphManager graphManager;

    public RepositoryGet(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    @Override
    public RepositoryGetResponse apply(RepositoryGetRequest request) {
        if(! request.opt().repositoryId().isPresent()) {
            return RepositoryGetResponse.builder().status400(Status400.builder().payload(Error.builder()
                    .code(Error.Code.ILLEGAL_REQUEST)
                    .token(log.tokenized().info("no repository id provided " + request))
                    .description("must provide a repository-id")
                    .build()).build()).build();
        }

        try {
            Optional<Repository> repository = this.graphManager.repository(request.repositoryId());
            if(repository.isPresent()) {
                return RepositoryGetResponse.builder().status200(Status200.builder()
                        .payload(repository.get())
                        .build()).build();
            } else {
                return RepositoryGetResponse.builder().status404(Status404.builder().payload(Error.builder()
                        .code(Error.Code.RESOURCE_NOT_FOUND)
                        .token(log.tokenized().info("no matching repository for " + request))
                        .build()).build()).build();
            }
        } catch (GraphManagerException e) {
            return RepositoryGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(log.tokenized().error("unexpected graph error getting repository, request was " + request, e))
                    .build()).build()).build();
        }
    }
}
