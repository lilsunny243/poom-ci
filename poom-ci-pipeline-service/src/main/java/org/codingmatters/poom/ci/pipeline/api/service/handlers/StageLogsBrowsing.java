package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineStageLogsGetRequest;
import org.codingmatters.poom.ci.pipeline.api.PipelineStageLogsGetResponse;
import org.codingmatters.poom.ci.pipeline.api.service.helpers.StageHelper;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLogQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.pipeline.api.types.LogLine;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StageLogsBrowsing implements Function<PipelineStageLogsGetRequest, PipelineStageLogsGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(StageLogsBrowsing.class);

    private final Repository<StageLog, StageLogQuery> logRepository;

    public StageLogsBrowsing(PoomCIRepository repository) {
        this.logRepository = repository.logRepository();
    }

    @Override
    public PipelineStageLogsGetResponse apply(PipelineStageLogsGetRequest request) {

        Rfc7233Pager<StageLog, StageLogQuery> pager = Rfc7233Pager.forRequestedRange(request.range())
                .unit("LogLine")
                .maxPageSize(100)
                .pager(this.logRepository);
        if(! StageHelper.isStageTypeValid(request.stageType())) {
            return PipelineStageLogsGetResponse.builder()
                    .status400(status -> status.payload(error -> error
                            .token(log.audit().tokenized().info("requested logs for pipeline {} stage {} with invalid stage type : %s",
                                    request.pipelineId(), request.stageName(), request.stageType()))
                            .code(Error.Code.ILLEGAL_REQUEST)
                            .description("invalid range type")
                    ))
                    .build();
        }
        try {
            Rfc7233Pager.Page<StageLog> page = pager.page(StageLogQuery.builder()
                    .withPipelineId(request.pipelineId())
                    .withStageName(request.stageName())
                    .build());

            if(! page.isValid()) {
                return PipelineStageLogsGetResponse.builder()
                        .status416(status -> status.payload(error -> error
                                .token(log.audit().tokenized().info("requested logs for pipeline {} stage {} with invalid range : %s",
                                        request.pipelineId(), request.stageName(), page.validationMessage()))
                                .code(Error.Code.ILLEGAL_RANGE_SPEC)
                                .description(page.validationMessage())
                        ))
                        .build();
            }

            if(page.isPartial()) {
                return PipelineStageLogsGetResponse.builder()
                        .status206(status -> status
                                .acceptRange(page.acceptRange())
                                .contentRange(page.contentRange())
                                .xPipelineId(request.pipelineId())
                                .xStageName(request.stageName())
                                .payload(this.toLogLines(page))
                        )
                        .build();
            } else {
                return PipelineStageLogsGetResponse.builder()
                        .status200(status -> status
                                .acceptRange(page.acceptRange())
                                .contentRange(page.contentRange())
                                .xPipelineId(request.pipelineId())
                                .xStageName(request.stageName())
                                .payload(this.toLogLines(page))
                        )
                        .build();
            }

        } catch (RepositoryException e) {
            return PipelineStageLogsGetResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .token(log.tokenized().error("error accessing log repository", e))
                            .code(Error.Code.UNEXPECTED_ERROR)
                    ))
                    .build();
        }
    }

    private List<LogLine> toLogLines(Rfc7233Pager.Page<StageLog> page) {
        return page.list().valueList().stream().map(entity -> entity.log()).collect(Collectors.toList());
    }
}
