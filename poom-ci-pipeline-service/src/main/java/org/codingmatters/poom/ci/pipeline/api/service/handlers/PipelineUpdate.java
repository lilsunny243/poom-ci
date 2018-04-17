package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelinePatchRequest;
import org.codingmatters.poom.ci.pipeline.api.PipelinePatchResponse;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Function;

public class PipelineUpdate implements Function<PipelinePatchRequest, PipelinePatchResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PipelineUpdate.class);

    private final Repository<Pipeline, String> pipelineRepository;

    public PipelineUpdate(PoomCIRepository repository) {
        this.pipelineRepository = repository.pipelineRepository();
    }

    @Override
    public PipelinePatchResponse apply(PipelinePatchRequest request) {
        try {
            Entity<Pipeline> entity = this.pipelineRepository.retrieve(request.pipelineId());
            if(entity == null) {
                return PipelinePatchResponse.builder()
                        .status404(status -> status.payload(error -> error
                                .token(log.audit().tokenized().info("pipeline {} not found", request.pipelineId()))
                                .code(Error.Code.RESOURCE_NOT_FOUND)
                                .description("pipeline not found")
                        ))
                        .build();
            }

            if(! request.opt().payload().exit().isPresent()) {
                return PipelinePatchResponse.builder()
                        .status400(status -> status.payload(error -> error
                                .token(log.audit().tokenized().info("trying to terminate pipeline {} without exit status",
                                        request.pipelineId()))
                                .code(Error.Code.ILLEGAL_RESOURCE_CHANGE)
                        ))
                        .build();
            }

            if(Status.Run.DONE.equals(entity.value().status().run())) {
                return PipelinePatchResponse.builder()
                        .status400(status -> status.payload(error -> error
                                .token(log.audit().tokenized().info("trying to terminate pipeline {} but it is already done",
                                        request.pipelineId()))
                                .code(Error.Code.ILLEGAL_RESOURCE_CHANGE)
                        ))
                        .build();
            }

            Entity<Pipeline> updated = this.pipelineRepository.update(entity, entity.value().withStatus(entity.value().status()
                    .withRun(Status.Run.DONE)
                    .withExit(Status.Exit.valueOf(request.payload().exit().name()))
                    .withFinished(LocalDateTime.now(ZoneOffset.UTC.normalized()))
            ));

            log.audit().info("pipeline {} done with exit status {}", request.pipelineId(), request.payload().exit());
            return PipelinePatchResponse.builder()
                    .status200(status -> status.payload(updated.value()))
                    .build();
        } catch (RepositoryException e) {
            return PipelinePatchResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .token(log.tokenized().error("error accessing pipeline repository", e))
                            .code(Error.Code.UNEXPECTED_ERROR)
                    ))
                    .build();
        }
    }
}
