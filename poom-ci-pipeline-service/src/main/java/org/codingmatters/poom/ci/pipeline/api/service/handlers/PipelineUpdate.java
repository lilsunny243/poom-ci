package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelinePatchRequest;
import org.codingmatters.poom.ci.pipeline.api.PipelinePatchResponse;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTermination;
import org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.Entity;

import java.util.Optional;
import java.util.function.Function;

public class PipelineUpdate implements Function<PipelinePatchRequest, PipelinePatchResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PipelineUpdate.class);

    private final Repository<Pipeline, PipelineQuery> pipelineRepository;

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

            Optional<PipelinePatchResponse> invalid = this.validate(request, entity);
            if(invalid.isPresent()) return invalid.get();

            Status.Run runStatus = Status.Run.valueOf(request.opt().payload().run().orElse(PipelineTermination.Run.DONE).name());
            Status.Exit exitStatus = request.opt().payload().exit().isPresent() ? Status.Exit.valueOf(request.payload().exit().name()) : null;

            Entity<Pipeline> updated = this.pipelineRepository.update(entity, entity.value().withStatus(entity.value().status()
                    .withRun(runStatus)
                    .withExit(exitStatus)
                    .withFinished(UTC.now())
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

    private Optional<PipelinePatchResponse> validate(PipelinePatchRequest request, Entity<Pipeline> entity) {
        if(! (request.opt().payload().run().isPresent() || request.opt().payload().exit().isPresent())) {
            return Optional.of(PipelinePatchResponse.builder()
                    .status400(status -> status.payload(error -> error
                            .token(log.audit().tokenized().info("must specify at least one of run or exit status : {}",
                                    request.pipelineId()))
                            .code(Error.Code.ILLEGAL_RESOURCE_CHANGE)
                    ))
                    .build());
        }

        if(request.opt().payload().exit().isPresent()) {
            if(! this.terminatingPipeline(request)) {
                return Optional.of(PipelinePatchResponse.builder()
                        .status400(status -> status.payload(error -> error
                                .token(log.audit().tokenized().info("can only set exit status if setting run status to DONE or null : {}",
                                        request.pipelineId()))
                                .code(Error.Code.ILLEGAL_RESOURCE_CHANGE)
                        ))
                        .build());
            }
        }

        if(PipelineTermination.Run.RUNNING.equals(request.opt().payload().run().orElse(PipelineTermination.Run.DONE))) {
            if(request.opt().payload().exit().isPresent()) {
                return Optional.of(PipelinePatchResponse.builder()
                        .status400(status -> status.payload(error -> error
                                .token(log.audit().tokenized().info("cannot set exit status when setting run status to RUNNING : {}",
                                        request.pipelineId()))
                                .code(Error.Code.ILLEGAL_RESOURCE_CHANGE)
                        ))
                        .build());
            }
        }

        if(Status.Run.DONE.equals(entity.value().status().run())) {
            return Optional.of(PipelinePatchResponse.builder()
                    .status400(status -> status.payload(error -> error
                            .token(log.audit().tokenized().info("trying to terminate pipeline {} but it is already done",
                                    request.pipelineId()))
                            .code(Error.Code.ILLEGAL_RESOURCE_CHANGE)
                    ))
                    .build());
        }
        return Optional.empty();
    }

    private boolean terminatingPipeline(PipelinePatchRequest request) {
        return PipelineTermination.Run.DONE.equals(request.opt().payload().run().orElse(PipelineTermination.Run.DONE));
    }
}
