package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineStagePatchRequest;
import org.codingmatters.poom.ci.pipeline.api.PipelineStagePatchResponse;
import org.codingmatters.poom.ci.pipeline.api.pipelinestagepatchresponse.Status200;
import org.codingmatters.poom.ci.pipeline.api.service.helpers.StageHelper;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStageQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.pipeline.api.types.StageStatus;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.function.Function;

public class StageUpdate implements Function<PipelineStagePatchRequest, PipelineStagePatchResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(StageUpdate.class);

    private final Repository<PipelineStage, PipelineStageQuery> stageRepository;

    public StageUpdate(PoomCIRepository repository) {
        this.stageRepository = repository.stageRepository();
    }

    @Override
    public PipelineStagePatchResponse apply(PipelineStagePatchRequest request) {
        if(!StageHelper.isStageTypeValid(request.stageType())) {
            return PipelineStagePatchResponse.builder()
                    .status400(status -> status.payload(error -> error
                            .token(log.audit().tokenized().info("request for updating status with an invalid stage type {}",
                                    request.stageType())
                            )
                            .code(Error.Code.RESOURCE_NOT_FOUND)
                            .description("stage type not found")
                    ))
                    .build();
        }
        try {
            PagedEntityList<PipelineStage> found = this.stageRepository.search(PipelineStageQuery.builder()
                            .withPipelineId(request.pipelineId())
                            .withName(request.stageName())
                            .withType(request.stageType())
                            .build(),
                    0,
                    0);

            if(found.total() == 0) {
                return PipelineStagePatchResponse.builder()
                        .status404(status -> status.payload(error -> error
                                .token(log.audit().tokenized().info("request for updating status of an unexistent stage {} in pipeline {}",
                                        request.stageName(), request.pipelineId())
                                )
                                .code(Error.Code.RESOURCE_NOT_FOUND)
                                .description("stage not found")
                        ))
                        .build();
            }

            Entity<PipelineStage> entity = found.get(0);

            if(StageStatus.Run.DONE.equals(entity.value().opt().stage().status().run().orElse(StageStatus.Run.DONE))) {
                return PipelineStagePatchResponse.builder().status400(status -> status.payload(error -> error
                        .token(log.audit().tokenized().info("stage {} of pipeline {} is already DONE",
                                request.stageName(),
                                request.pipelineId()))
                        .code(Error.Code.ILLEGAL_RESOURCE_CHANGE)
                        .description("cannot change state of an already done stage")
                ))
                        .build();
            }

            entity = this.stageRepository.update(
                    entity,
                    entity.value().withStage(entity.value().stage().withStatus(StageStatus.builder()
                            .run(StageStatus.Run.DONE)
                            .exit(StageStatus.Exit.valueOf(request.payload().exit().name()))
                            .build()))
            );

            return PipelineStagePatchResponse.builder()
                    .status200(Status200.builder().payload(entity.value().stage()).build())
                    .build();

        } catch (RepositoryException e) {
            return PipelineStagePatchResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .token(log.tokenized().error("error accessing stage repository", e))
                            .code(Error.Code.UNEXPECTED_ERROR)
                    ))
                    .build();
        }
    }
}
