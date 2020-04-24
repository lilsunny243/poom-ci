package org.codingmatters.poom.ci.dependency.flat.handlers;

import org.codingmatters.poom.ci.dependency.api.RepositoryGraphGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryGraphGetResponse;
import org.codingmatters.poom.ci.dependency.api.repositorygraphgetresponse.Status200;
import org.codingmatters.poom.ci.dependency.api.repositorygraphgetresponse.Status400;
import org.codingmatters.poom.ci.dependency.api.repositorygraphgetresponse.Status404;
import org.codingmatters.poom.ci.dependency.api.types.Error;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;
import org.codingmatters.poom.ci.dependency.flat.GraphManagerException;
import org.codingmatters.poom.ci.dependency.flat.NoSuchRepositoryException;
import org.codingmatters.poom.ci.dependency.flat.RepositoryGraphProcessor;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.function.Function;

public class RepositoryGraphGet implements Function<RepositoryGraphGetRequest, RepositoryGraphGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RepositoryGraphGet.class);
    private final GraphManager graphManager;
    private final RepositoryGraphProcessor processor;

    public RepositoryGraphGet(GraphManager graphManager) {
        this.graphManager = graphManager;
        this.processor = new RepositoryGraphProcessor(this.graphManager);
    }

    @Override
    public RepositoryGraphGetResponse apply(RepositoryGraphGetRequest request) {
        if(! request.opt().root().isPresent()) {
            return RepositoryGraphGetResponse.builder().status400(Status400.builder().payload(Error.builder()
                    .code(Error.Code.ILLEGAL_REQUEST)
                    .token(log.tokenized().info("no root repository provided {}", request))
                    .description("must provide a root repository")
                    .build()).build()).build();
        }
        try {
            return RepositoryGraphGetResponse.builder().status200(Status200.builder()
                    .payload(this.processor.graph(request.root()))
                    .build()).build();
        } catch (GraphManagerException e) {
            return RepositoryGraphGetResponse.builder().status404(Status404.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(log.tokenized().error("unexpected exception while calculating graph " + request, e))
                    .description("unexpected error, see logs")
                    .build()).build()).build();
        } catch (NoSuchRepositoryException e) {
            return RepositoryGraphGetResponse.builder().status404(Status404.builder().payload(Error.builder()
                    .code(Error.Code.RESOURCE_NOT_FOUND)
                    .token(log.tokenized().info("repository not found for root {}", request))
                    .description("root repository doesn't exist")
                    .build()).build()).build();
        }
    }
}
