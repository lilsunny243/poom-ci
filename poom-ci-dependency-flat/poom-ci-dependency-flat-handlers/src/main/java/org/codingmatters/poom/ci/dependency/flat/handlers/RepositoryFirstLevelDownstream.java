package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryJustNextDownstreamRepositoriesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryJustNextDownstreamRepositoriesGetResponse;
import org.codingmatters.poom.ci.dependency.api.repositoryjustnextdownstreamrepositoriesgetresponse.Status200;
import org.codingmatters.poom.ci.dependency.api.repositoryjustnextdownstreamrepositoriesgetresponse.Status400;
import org.codingmatters.poom.ci.dependency.api.repositoryjustnextdownstreamrepositoriesgetresponse.Status404;
import org.codingmatters.poom.ci.dependency.api.repositoryjustnextdownstreamrepositoriesgetresponse.Status500;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.flat.*;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.function.Function;

public class RepositoryFirstLevelDownstream implements Function<RepositoryJustNextDownstreamRepositoriesGetRequest, RepositoryJustNextDownstreamRepositoriesGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RepositoryFirstLevelDownstream.class);

    private final GraphManager graphManager;

    public RepositoryFirstLevelDownstream(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    @Override
    public RepositoryJustNextDownstreamRepositoriesGetResponse apply(RepositoryJustNextDownstreamRepositoriesGetRequest request) {
        if(! request.opt().repositoryId().isPresent()) {
            return RepositoryJustNextDownstreamRepositoriesGetResponse.builder().status400(Status400.builder().payload(Error.builder()
                    .code(Error.Code.ILLEGAL_REQUEST)
                    .token(log.tokenized().info("unexpected error while calculating downstream : {}", request))
                    .description("must provide a repository id")
                    .build()).build()).build();
        }
        try {
            return RepositoryJustNextDownstreamRepositoriesGetResponse.builder().status200(Status200.builder()
                    .payload(new FirstLevelDownstreamProcessor(this.graphManager).downstream(request.repositoryId()))
                    .build()).build();
        } catch (NoSuchRepositoryException e) {
            return RepositoryJustNextDownstreamRepositoriesGetResponse.builder().status404(Status404.builder().payload(Error.builder()
                    .code(Error.Code.RESOURCE_NOT_FOUND)
                    .token(log.tokenized().info("unexpected error while calculating downstream : " + request, e))
                    .description("repository not found")
                    .build()).build()).build();
        } catch (GraphManagerException e) {
            return RepositoryJustNextDownstreamRepositoriesGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(log.tokenized().error("unexpected error while calculating downstream : " + request, e))
                    .description("unexpected error see logs")
                    .build()).build()).build();
        }
    }
}
