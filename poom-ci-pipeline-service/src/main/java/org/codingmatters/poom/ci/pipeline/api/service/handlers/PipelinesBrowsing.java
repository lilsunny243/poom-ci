package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelinesGetRequest;
import org.codingmatters.poom.ci.pipeline.api.PipelinesGetResponse;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.property.query.PropertyQueryParser;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;

import java.util.Optional;
import java.util.function.Function;

public class PipelinesBrowsing implements Function<PipelinesGetRequest, PipelinesGetResponse> {

    static private CategorizedLogger log = CategorizedLogger.getLogger(PipelinesBrowsing.class);

    private final Repository<Pipeline, PropertyQuery> repository;

    public PipelinesBrowsing(PoomCIRepository repository) {
        this.repository = repository.pipelineRepository();
    }

    @Override
    public PipelinesGetResponse apply(PipelinesGetRequest request) {

        Rfc7233Pager<Pipeline, PropertyQuery>  pager = Rfc7233Pager
                .forRequestedRange(request.range())
                .unit("Pipeline")
                .maxPageSize(100)
                .pager(this.repository);

        try {
            Rfc7233Pager.Page<Pipeline> page = pager.page(this.parseQuery(request));
            if(! page.isValid()) {
                return this.invalidRange(request, page);
            } else {
                return this.listResponse(page);
            }
        } catch (RepositoryException e) {
            return this.repositoryException(e);
        }
    }

    private Optional<PropertyQuery> parseQuery(PipelinesGetRequest request) {
        if(request.opt().filter().isPresent() || request.opt().orderBy().isPresent()) {
            return Optional.of(PropertyQuery.builder().filter(request.filter()).sort(request.orderBy()).build());
        } else {
            return Optional.empty();
        }
    }

    private PipelinesGetResponse invalidRange(PipelinesGetRequest request, Rfc7233Pager.Page<Pipeline> page) {
        return PipelinesGetResponse.builder()
                .status416(status -> status
                        .acceptRange(page.acceptRange())
                        .contentRange(page.contentRange())
                        .payload(error -> error
                                .token(log.audit().tokenized().error("invalid range query", request))
                                .code(Error.Code.ILLEGAL_RANGE_SPEC)
                                .description(page.validationMessage())
                        )
                )
                .build();
    }

    private PipelinesGetResponse repositoryException(RepositoryException e) {
        return PipelinesGetResponse.builder()
                .status500(status -> status.payload(error -> error
                        .token(log.tokenized().error("failed paging pipelines, unexpected repository exception", e))
                        .code(Error.Code.UNEXPECTED_ERROR)
                        .description("unexpected error occurred, see logs.")
                ))
                .build();
    }

    private PipelinesGetResponse listResponse(Rfc7233Pager.Page<Pipeline> page) {
        if(page.isPartial()) {
            log.audit().info("returning partial pipeline list");
            return PipelinesGetResponse.builder()
                    .status206(status -> status
                            .acceptRange(page.acceptRange())
                            .contentRange(page.contentRange())
                            .payload(page.list().valueList())
                    )
                    .build();
        } else {
            log.audit().info("returning complete pipeline list");
            return PipelinesGetResponse.builder()
                    .status200(status -> status
                            .acceptRange(page.acceptRange())
                            .contentRange(page.contentRange())
                            .payload(page.list().valueList())
                    )
                    .build();
        }
    }
}
