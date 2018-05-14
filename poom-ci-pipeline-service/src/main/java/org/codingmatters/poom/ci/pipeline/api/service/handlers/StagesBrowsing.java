package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelineStagesGetRequest;
import org.codingmatters.poom.ci.pipeline.api.PipelineStagesGetResponse;
import org.codingmatters.poom.ci.pipeline.api.service.helpers.StageHelper;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStageQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StagesBrowsing implements Function<PipelineStagesGetRequest, PipelineStagesGetResponse> {
    static private CategorizedLogger log = CategorizedLogger.getLogger(StagesBrowsing.class);

    private final Repository<PipelineStage, PipelineStageQuery> stageRepository;

    public StagesBrowsing(PoomCIRepository repository) {
        this.stageRepository = repository.stageRepository();
    }

    @Override
    public PipelineStagesGetResponse apply(PipelineStagesGetRequest request) {

        Rfc7233Pager<PipelineStage, PipelineStageQuery> pager = Rfc7233Pager
                .forRequestedRange(request.range())
                .unit("Stage")
                .maxPageSize(500)
                .pager(this.stageRepository);

        if(! StageHelper.isStageTypeValid(request.stageType())) {
            return PipelineStagesGetResponse.builder()
                        .status416(status -> status.payload(error -> error
                                .token(log.audit().tokenized().info("stages requested without an invalid stage type : {}", request.stageType()))
                                .code(Error.Code.ILLEGAL_RANGE_SPEC)
                                .description("stages requested without an invalid stage type (see logs).")
                        ))
                        .build();
        }

        try {
            Rfc7233Pager.Page<PipelineStage> page = pager.page(PipelineStageQuery.builder()
                    .withPipelineId(request.pipelineId())
                    .withType(request.stageType())
                    .build());
            if(! page.isValid()) {
                return PipelineStagesGetResponse.builder()
                        .status416(status -> status.payload(error -> error
                                .token(log.audit().tokenized().info("stages requested with invalid range : {}", page.validationMessage()))
                                .code(Error.Code.ILLEGAL_RANGE_SPEC)
                                .description(page.validationMessage())
                        ))
                        .build();
            }

            if(page.isPartial()) {
                log.audit().info("returning partial stage list {} for pipeline {}", page.contentRange(), request.pipelineId());
                return PipelineStagesGetResponse.builder()
                        .status206(status -> status
                                .payload(this.asStageList(page))
                                .acceptRange(page.acceptRange())
                                .contentRange(page.contentRange())
                        )
                        .build();
            } else {
                log.audit().info("returning complete stage list {} for pipeline {}", page.contentRange(), request.pipelineId());
                return PipelineStagesGetResponse.builder()
                        .status200(status -> status
                                .payload(this.asStageList(page))
                                .acceptRange(page.acceptRange())
                                .contentRange(page.contentRange())
                        )
                        .build();
            }

        } catch (RepositoryException e) {
            return PipelineStagesGetResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .token(log.tokenized().error("error accessing repository", e))
                            .code(Error.Code.UNEXPECTED_ERROR)
                    ))
                    .build();
        }
    }

    private List<Stage> asStageList(Rfc7233Pager.Page<PipelineStage> page) {
        return page.list().valueList().stream().map(pipelineStage -> pipelineStage.stage()).collect(Collectors.toList());
    }
}
