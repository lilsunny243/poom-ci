package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineStageLogsPatchRequest;
import org.codingmatters.poom.ci.pipeline.api.PipelineStageLogsPatchResponse;
import org.codingmatters.poom.ci.pipeline.api.service.helpers.StageHelper;
import org.codingmatters.poom.ci.pipeline.api.service.repository.LogStore;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.types.AppendedLogLine;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.ci.pipeline.api.types.StageStatus;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.rest.api.Processor;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StageLogsAppend implements Function<PipelineStageLogsPatchRequest, PipelineStageLogsPatchResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(StageLogsAppend.class);

    private final Repository<PipelineStage, PropertyQuery> stageRepository;
    private final LogStore logStore;

    public StageLogsAppend(PoomCIRepository repository) {
        this.stageRepository = repository.stageRepository();
        this.logStore = repository.logStore();
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

        PagedEntityList<PipelineStage> stageSearch = this.stageRepository.search(
                PropertyQuery.builder()
                        .filter(String.format(
                                "pipelineId == '%s' && stage.name == '%s'",
                                request.pipelineId(),
                                request.stageName()
                                ))
                        .build(),
                0, 0);
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
        } else {
            invalid = Optional.empty();
        }
        return invalid;
    }

    private void appendLogs(PipelineStageLogsPatchRequest request) throws RepositoryException {
        List<String> lines = request.payload().stream().map(AppendedLogLine::content).collect(Collectors.toList());
        try {
            this.logStore.segment(request.pipelineId(), Stage.StageType.valueOf(request.stageType().toUpperCase()), request.stageName())
                    .append(lines.toArray(new String[lines.size()]));
        } catch (IOException e) {
            throw new RepositoryException("error storing log lines " + lines, e);
        }
        log.audit().trace("appended {} lines to pipeline log {} stage {}/{}",
                lines.size(),
                request.pipelineId(),
                request.stageType(),
                request.stageName()
        );
    }
}
