package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineGetRequest;
import org.codingmatters.poom.ci.pipeline.api.PipelineGetResponse;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;

import java.util.function.Function;

public class PipelineGet implements Function<PipelineGetRequest, PipelineGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PipelineGet.class);

    private final Repository<Pipeline, String> pipelineRepository;

    public PipelineGet(Repository<Pipeline, String> pipelineRepository) {
        this.pipelineRepository = pipelineRepository;
    }

    @Override
    public PipelineGetResponse apply(PipelineGetRequest request) {
        try {
            Entity<Pipeline> pipeline = this.pipelineRepository.retrieve(request.pipelineId());
            if(pipeline != null) {
                return PipelineGetResponse.builder()
                    .status200(status -> status
                            .payload(pipeline.value())
                            .xEntityId(pipeline.id())
                    )
                    .build();
            } else {
                return PipelineGetResponse.builder()
                    .status404(status -> status.payload(error -> error
                            .token(log.audit().tokenized().info("not such pipeline {}", request.pipelineId()))
                            .code(Error.Code.RESOURCE_NOT_FOUND)
                            .description("no pipeline found with the given id")
                    ))
                    .build();
            }
        } catch (RepositoryException e) {
            return PipelineGetResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .token(log.tokenized().error("problem with pipeline repository", e))
                            .code(Error.Code.UNEXPECTED_ERROR)
                    ))
                    .build();
        }
    }
}
