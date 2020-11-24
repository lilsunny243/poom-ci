package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryJustNextPropagationCandidatesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryJustNextPropagationCandidatesGetResponse;
import org.codingmatters.poom.ci.dependency.api.repositoryjustnextpropagationcandidatesgetresponse.*;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;
import org.codingmatters.poom.ci.dependency.flat.GraphManagerException;
import org.codingmatters.poom.ci.dependency.flat.NoSuchRepositoryException;
import org.codingmatters.poom.ci.dependency.flat.PropagationCandidatesProcessor;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.function.Function;

public class PropagationCandidatesFirstLevel implements Function<RepositoryJustNextPropagationCandidatesGetRequest, RepositoryJustNextPropagationCandidatesGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PropagationCandidatesFirstLevel.class);
    
    private final GraphManager graphManager;

    public PropagationCandidatesFirstLevel(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    @Override
    public RepositoryJustNextPropagationCandidatesGetResponse apply(RepositoryJustNextPropagationCandidatesGetRequest request) {
        if(! request.opt().repositoryId().isPresent()) {
            return RepositoryJustNextPropagationCandidatesGetResponse.builder().status400(Status400.builder().payload(Error.builder()
                    .code(Error.Code.ILLEGAL_REQUEST)
                    .token(log.tokenized().info("unexpected error while calculating first level propagation candidates : {}", request))
                    .description("must provide a repository id")
                    .build()).build()).build();
        }
        try {
            return RepositoryJustNextPropagationCandidatesGetResponse.builder().status200(Status200.builder()
                    .payload(new PropagationCandidatesProcessor(this.graphManager, PropagationCandidatesProcessor.Restriction.FIRST_LEVEL).process(request.repositoryId()))
                    .build()).build();
        } catch (NoSuchRepositoryException e) {
            return RepositoryJustNextPropagationCandidatesGetResponse.builder().status404(Status404.builder().payload(Error.builder()
                    .code(Error.Code.RESOURCE_NOT_FOUND)
                    .token(log.tokenized().info("unexpected error while calculating first level propagation candidates : " + request, e))
                    .description("repository not found")
                    .build()).build()).build();
        } catch (GraphManagerException e) {
            return RepositoryJustNextPropagationCandidatesGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(log.tokenized().error("unexpected error while calculating first level propagation candidates : " + request, e))
                    .description("unexpected error see logs")
                    .build()).build()).build();
        }
    }
}
