package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineStageGetRequest;
import org.codingmatters.poom.ci.pipeline.api.PipelineStageGetResponse;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStageQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.Optional;
import java.util.function.Function;

public class StageGet implements Function<PipelineStageGetRequest, PipelineStageGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(StageGet.class);

    private final Repository<PipelineStage, PipelineStageQuery> stageRepository;

    public StageGet(PoomCIRepository repository) {
        this.stageRepository = repository.stageRepository();
    }

    @Override
    public PipelineStageGetResponse apply(PipelineStageGetRequest request) {
        Optional<PipelineStageGetResponse> invalidRequest = this.validate(request);
        if(invalidRequest.isPresent()) return invalidRequest.get();

        try {
            PagedEntityList<PipelineStage> found = this.stageRepository.search(this.parseQuery(request), 0, 1);
            if(! found.isEmpty()) {
                log.audit().info("successful request for pipeline {} stage {}", request.pipelineId(), request.stageName());
                return PipelineStageGetResponse.builder()
                        .status200(status -> status
                                .payload(found.get(0).value().stage())
                                .xEntityId(found.get(0).value().stage().name())
                                .xPipelineId(found.get(0).value().pipelineId())
                        )
                        .build();
            } else {
                return PipelineStageGetResponse.builder()
                    .status404(status -> status.payload(error -> error
                            .token(log.audit().tokenized().info("no stage found for pipeline {} with name {}", request.pipelineId(), request.stageName()))
                            .code(Error.Code.RESOURCE_NOT_FOUND)
                    ))
                    .build();
            }
        } catch (RepositoryException e) {
            return PipelineStageGetResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .token(log.tokenized().error("error accessing repository", e))
                            .code(Error.Code.UNEXPECTED_ERROR)
                    ))
                    .build();
        }
    }

    private Optional<PipelineStageGetResponse> validate(PipelineStageGetRequest request) {
        if(! request.opt().pipelineId().isPresent()) {
            return Optional.of(PipelineStageGetResponse.builder()
                    .status404(status -> status.payload(error -> error
                            .token(log.audit().tokenized().info("to retrieve a stage, you must provide a pipeline id"))
                            .code(Error.Code.RESOURCE_NOT_FOUND)
                    ))
                    .build());
        } else if(! request.opt().pipelineId().isPresent()) {
            return Optional.of(PipelineStageGetResponse.builder()
                    .status404(status -> status.payload(error -> error
                            .token(log.audit().tokenized().info("to retrieve a stage, you must provide a stage name"))
                            .code(Error.Code.RESOURCE_NOT_FOUND)
                    ))
                    .build());
        }
        return Optional.empty();
    }

    private PipelineStageQuery parseQuery(PipelineStageGetRequest request) {
        return PipelineStageQuery.builder()
                .withPipelineId(request.pipelineId())
                .withName(request.stageName())
                .withType(request.stageType())
                .build();
    }
}
