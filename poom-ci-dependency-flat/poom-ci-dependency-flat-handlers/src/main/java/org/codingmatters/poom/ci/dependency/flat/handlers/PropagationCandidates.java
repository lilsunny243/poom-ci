package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryPropagationCandidatesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryPropagationCandidatesGetResponse;
import org.codingmatters.poom.ci.dependency.api.repositorypropagationcandidatesgetresponse.*;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;
import org.codingmatters.poom.ci.dependency.flat.GraphManagerException;
import org.codingmatters.poom.ci.dependency.flat.NoSuchRepositoryException;
import org.codingmatters.poom.ci.dependency.flat.PropagationCandidatesProcessor;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.function.Function;

public class PropagationCandidates implements Function<RepositoryPropagationCandidatesGetRequest, RepositoryPropagationCandidatesGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PropagationCandidates.class);
    
    private final GraphManager graphManager;

    public PropagationCandidates(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    @Override
    public RepositoryPropagationCandidatesGetResponse apply(RepositoryPropagationCandidatesGetRequest request) {
        if(! request.opt().repositoryId().isPresent()) {
            return RepositoryPropagationCandidatesGetResponse.builder().status400(Status400.builder().payload(Error.builder()
                    .code(Error.Code.ILLEGAL_REQUEST)
                    .token(log.tokenized().info("unexpected error while calculating downstream : {}", request))
                    .description("must provide a repository id")
                    .build()).build()).build();
        }
        try {
            return RepositoryPropagationCandidatesGetResponse.builder().status200(Status200.builder()
                    .payload(new PropagationCandidatesProcessor(this.graphManager).candidates(request.repositoryId()))
                    .build()).build();
        } catch (NoSuchRepositoryException e) {
            return RepositoryPropagationCandidatesGetResponse.builder().status404(Status404.builder().payload(Error.builder()
                    .code(Error.Code.RESOURCE_NOT_FOUND)
                    .token(log.tokenized().info("unexpected error while calculating propagation candidates : " + request, e))
                    .description("repository not found")
                    .build()).build()).build();
        } catch (GraphManagerException e) {
            return RepositoryPropagationCandidatesGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(log.tokenized().error("unexpected error while calculating propagation candidates : " + request, e))
                    .description("unexpected error see logs")
                    .build()).build()).build();
        }
    }
}
