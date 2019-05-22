package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineStagesPostRequest;
import org.codingmatters.poom.ci.pipeline.api.PipelineStagesPostResponse;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineQuery;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStageQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.pipeline.api.types.*;
import org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.rest.api.Processor;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StageCreate implements Function<PipelineStagesPostRequest, PipelineStagesPostResponse> {
    static private CategorizedLogger log = CategorizedLogger.getLogger(StageCreate.class);

    private final Repository<Pipeline, PipelineQuery> pipelineRepository;
    private final Repository<PipelineStage, PipelineStageQuery> stageRepository;

    public StageCreate(PoomCIRepository repository) {
        this.pipelineRepository = repository.pipelineRepository();
        this.stageRepository = repository.stageRepository();
    }

    @Override
    public PipelineStagesPostResponse apply(PipelineStagesPostRequest request) {
        try {
            Optional<PipelineStagesPostResponse> invalidRequest = this.validate(request);
            if(invalidRequest.isPresent()) {
                return invalidRequest.get();
            }

            StageCreation creation = request.payload();
            Entity<PipelineStage> created = this.stageRepository.create(PipelineStage.builder()
                    .pipelineId(request.pipelineId())
                    .stage(stage -> stage
                            .name(creation.name())
                            .status(status -> status.run(StageStatus.Run.RUNNING))
                            .stageType(Stage.StageType.valueOf(request.stageType().toUpperCase()))
                            .triggered(UTC.now())
                    )
                    .build());

            log.audit().info("running stage {} created for running pipeline {}", created.value().stage().name(), request.pipelineId());

            return PipelineStagesPostResponse.builder()
                    .status201(status -> status
                            .xEntityId(created.value().stage().name())
                            .location(String.format("%s/pipelines/%s/stages/%s",
                                    Processor.Variables.API_PATH.token(),
                                    request.pipelineId(),
                                    created.value().stage().name()
                            ))
                    )
                    .build();
        } catch (RepositoryException e) {
            return PipelineStagesPostResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .code(Error.Code.UNEXPECTED_ERROR)
                            .token(log.tokenized().error("cannot access repository", e))))
                    .build();
        }
    }

    private Optional<PipelineStagesPostResponse> validate(PipelineStagesPostRequest request) throws RepositoryException {
        if(! request.opt().stageType().isPresent()) {
            return Optional.of(PipelineStagesPostResponse.builder()
                    .status400(status -> status.payload(error -> error
                            .code(Error.Code.ILLEGAL_RESOURCE_CREATION)
                            .description("cannot create stage without specifying a stage type within " + Stage.StageType.values())
                            .token(log.audit().tokenized().info("no stage type specified"))
                    ))
                    .build());
        }

        if( !Arrays.stream(Stage.StageType.values()).map(stageType -> stageType.name().toUpperCase()).collect(Collectors.toSet()).contains(request.stageType().toUpperCase())) {
            return Optional.of(PipelineStagesPostResponse.builder()
                    .status400(status -> status.payload(error -> error
                            .code(Error.Code.ILLEGAL_RESOURCE_CREATION)
                            .token(log.audit().tokenized().info("the stage type {} doesn't exist", request.stageType()))
                            .description("cannot create stage without specifying a stage type within " + Stage.StageType.values())
                    ))
                    .build());
        }

        Entity<Pipeline> pipeline = this.pipelineRepository.retrieve(request.pipelineId());
        if(pipeline == null) {
            return Optional.of(PipelineStagesPostResponse.builder()
                    .status400(status -> status.payload(error -> error
                            .code(Error.Code.ILLEGAL_RESOURCE_CREATION)
                            .description("cannot create stage for unexistent pipeline")
                            .token(log.audit().tokenized().info("pieline with id {} doesn't exist", request.pipelineId()))
                    ))
                    .build());
        }

        if(! this.pipelineIsRunning(pipeline)) {
            return Optional.of(PipelineStagesPostResponse.builder()
                    .status400(status -> status.payload(error -> error
                            .code(Error.Code.ILLEGAL_RESOURCE_CREATION)
                            .description("cannot create stage for a pipeline that is not running")
                            .token(log.audit().tokenized().info("pipeline with id {} run state must be RUNNING, was {}",
                                    request.pipelineId(), pipeline.value().status().run()
                            ))
                    ))
                    .build());
        }

        if(this.stageAlreadyExists(request)) {
            return Optional.of(PipelineStagesPostResponse.builder()
                    .status400(status -> status.payload(error -> error
                            .code(Error.Code.ILLEGAL_RESOURCE_CREATION)
                            .description("state already exists")
                            .token(log.audit().tokenized().info("stage {} already exist in pipeline {}",
                                    request.payload().name(), request.pipelineId()
                            ))
                    ))
                    .build());
        }

        if(this.aStageIsAlreadyRunning(request)) {
            return Optional.of(PipelineStagesPostResponse.builder()
                    .status400(status -> status.payload(error -> error
                            .code(Error.Code.ILLEGAL_RESOURCE_CREATION)
                            .description("there's already a running stage")
                            .token(log.audit().tokenized().info("there is already a running stage in pipeline {}",
                                    request.payload().name(), request.pipelineId()
                            ))
                    ))
                    .build());
        }

        return Optional.empty();
    }

    private boolean aStageIsAlreadyRunning(PipelineStagesPostRequest request) throws RepositoryException {
        return this.stageRepository.search(PipelineStageQuery.builder()
                .withPipelineId(request.pipelineId())
                .withRunningStatus(PipelineStageQuery.WithRunningStatus.RUNNING)
                .build(), 0, 0).total() > 0;
    }

    private boolean stageAlreadyExists(PipelineStagesPostRequest request) throws RepositoryException {
        return this.stageRepository.search(PipelineStageQuery.builder()
                .withPipelineId(request.pipelineId())
                .withType(request.stageType().toUpperCase())
                .withName(request.payload().name())
                .build(), 0, 0).total() > 0;
    }

    private boolean pipelineIsRunning(Entity<Pipeline> pipeline) {
        return Status.Run.RUNNING.equals(pipeline.value().opt().status().run().orElse(Status.Run.DONE));
    }
}
