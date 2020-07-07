package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryPutRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryPutResponse;
import org.codingmatters.poom.ci.dependency.api.repositoryputresponse.Status200;
import org.codingmatters.poom.ci.dependency.api.repositoryputresponse.Status400;
import org.codingmatters.poom.ci.dependency.api.repositoryputresponse.Status500;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;
import org.codingmatters.poom.ci.dependency.flat.GraphManagerException;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.Optional;
import java.util.function.Function;

public class RepositoryCreateOrUpdate implements Function<RepositoryPutRequest, RepositoryPutResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RepositoryCreateOrUpdate.class);

    private final GraphManager graphManager;

    public RepositoryCreateOrUpdate(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    @Override
    public RepositoryPutResponse apply(RepositoryPutRequest request) {
        if(! request.opt().repositoryId().isPresent()) {
            return RepositoryPutResponse.builder().status400(Status400.builder().payload(Error.builder()
                    .code(Error.Code.ILLEGAL_REQUEST)
                    .token(log.tokenized().info("no repository id specified : {}", request))
                    .description("must provide a repository-id")
                    .build()).build()).build();
        }
        if(! request.opt().payload().isPresent()) {
            return RepositoryPutResponse.builder().status400(Status400.builder().payload(Error.builder()
                    .code(Error.Code.ILLEGAL_REQUEST)
                    .token(log.tokenized().info("no paylod specified : {}", request))
                    .description("must provide a repository specification in paylod")
                    .build()).build()).build();
        }
        try {
            this.graphManager.index(request.payload());
            Repository repository = this.graphManager.repository(request.repositoryId()).get();
            log.info("indexed repository {}", repository);
            return RepositoryPutResponse.builder().status200(Status200.builder()
                    .payload(repository)
                    .build()).build();
        } catch (GraphManagerException e) {
            return RepositoryPutResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.ILLEGAL_REQUEST)
                    .token(log.tokenized().error("unexpected error while indexing repository : " + request, e))
                    .description("unexpected error indexing repository, see logs")
                    .build()).build()).build();
        }
    }
}
