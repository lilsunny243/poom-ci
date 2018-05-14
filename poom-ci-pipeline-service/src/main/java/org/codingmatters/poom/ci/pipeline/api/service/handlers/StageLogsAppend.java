package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineStageLogsPatchRequest;
import org.codingmatters.poom.ci.pipeline.api.PipelineStageLogsPatchResponse;
import org.codingmatters.poom.ci.pipeline.api.service.helpers.StageHelper;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStageQuery;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLogQuery;
import org.codingmatters.poom.ci.pipeline.api.types.*;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.rest.api.Processor;

import java.util.Optional;
import java.util.function.Function;

public class StageLogsAppend implements Function<PipelineStageLogsPatchRequest, PipelineStageLogsPatchResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(StageLogsAppend.class);

    private final Repository<PipelineStage, PipelineStageQuery> stageRepository;
    private final Repository<StageLog, StageLogQuery> logRepository;

    public StageLogsAppend(PoomCIRepository repository) {
        this.stageRepository = repository.stageRepository();
        this.logRepository = repository.logRepository();
    }

    @Override
    public PipelineStageLogsPatchResponse apply(PipelineStageLogsPatchRequest request) {
        try {
            Optional<PipelineStageLogsPatchResponse> invalid = this.validate(request);
            if(invalid.isPresent()) {
                return invalid.get();
            }

            this.appendLogs(request);

            return PipelineStageLogsPatchResponse.builder()
                    .status201(status -> status
                            .location(String.format("%s/pipelines/%s/stages/%s/logs",
                                    Processor.Variables.API_PATH.token(),
                                    request.pipelineId(),
                                    request.stageName()
                            ))
                    )
                    .build();

        } catch (RepositoryException e) {
            return PipelineStageLogsPatchResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .token(log.tokenized().error("error accessing log repository", e))
                            .code(Error.Code.UNEXPECTED_ERROR)
                    ))
                    .build();
        }
    }

    private Optional<PipelineStageLogsPatchResponse> validate(PipelineStageLogsPatchRequest request) throws RepositoryException {
        Optional<PipelineStageLogsPatchResponse> invalid;

        if(!StageHelper.isStageTypeValid(request.stageType())) {
            return Optional.of(PipelineStageLogsPatchResponse.builder()
                    .status400(status -> status.payload(error -> error
                            .token(log.audit().tokenized().info("stage log append request on invalid stage type {} for pipeline {}",
                                    request.stageType(),
                                    request.pipelineId()))
                            .code(Error.Code.RESOURCE_NOT_FOUND)
                            .description("invalid stage type")
                    ))
                    .build());
        }

        PagedEntityList<PipelineStage> stageSearch = this.stageRepository.search(PipelineStageQuery.builder().withPipelineId(request.pipelineId()).withName(request.stageName()).build(), 0, 0);
        if(stageSearch.total() == 0) {
            invalid = Optional.of(PipelineStageLogsPatchResponse.builder()
                    .status404(status -> status.payload(error -> error
                            .token(log.audit().tokenized().info("stage log append request on non existing stage {} for pipeline {}",
                                    request.stageName(),
                                    request.pipelineId()))
                            .code(Error.Code.RESOURCE_NOT_FOUND)
                            .description("must provide an existing pipeline stage")
                    ))
                    .build());
        } else if(StageStatus.Run.DONE.equals(stageSearch.get(0).value().stage().status().run())) {
            invalid = Optional.of(PipelineStageLogsPatchResponse.builder()
                    .status400(status -> status.payload(error -> error
                            .token(log.audit().tokenized().info("stage log append request on DONE stage {} for pipeline {}",
                                    request.stageName(), request.pipelineId()))
                            .code(Error.Code.ILLEGAL_COLLECTION_CHANGE)
                            .description("cannot add logs to a done stage")
                    ))
                    .build());
        } else {
            invalid = Optional.empty();
        }
        return invalid;
    }

    private void appendLogs(PipelineStageLogsPatchRequest request) throws RepositoryException {
        long logCount = this.logRepository.search(StageLogQuery.builder()
                .withPipelineId(request.pipelineId())
                .withStageName(request.stageName())
                .withStageType(request.stageType())
                .build(), 0, 0).total();
        long nextLine = logCount;

        for (AppendedLogLine logLine : request.payload()) {
            nextLine++;

            log.debug("append log: ");
            LogLine logEntry = LogLine.builder()
                    .line(nextLine)
                    .content(logLine.content())
                    .build();
            this.logRepository.create(StageLog.builder()
                    .log(logEntry)
                    .pipelineId(request.pipelineId())
                    .stageName(request.stageName())
                    .stageType(Stage.StageType.valueOf(request.stageType().toUpperCase()))
                    .build());
        }

        log.audit().info("appended {} lines of log to pipeline {} stage {}",
                nextLine - logCount,
                request.pipelineId(),
                request.stageName()
        );
    }
}
