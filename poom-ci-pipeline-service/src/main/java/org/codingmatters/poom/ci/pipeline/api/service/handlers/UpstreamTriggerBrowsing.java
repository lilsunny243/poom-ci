package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.UpstreamBuildTriggersGetRequest;
import org.codingmatters.poom.ci.pipeline.api.UpstreamBuildTriggersGetResponse;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.UpstreamBuildQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.triggers.UpstreamBuild;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;

import java.util.function.Function;

public class UpstreamTriggerBrowsing implements Function<UpstreamBuildTriggersGetRequest, UpstreamBuildTriggersGetResponse> {
    static private CategorizedLogger log = CategorizedLogger.getLogger(UpstreamTriggerBrowsing.class);

    private final Repository<UpstreamBuild, PropertyQuery> repository;

    public UpstreamTriggerBrowsing(PoomCIRepository repository) {
        this.repository = repository.upstreamBuildRepository();
    }

    @Override
    public UpstreamBuildTriggersGetResponse apply(UpstreamBuildTriggersGetRequest request) {
        Rfc7233Pager<UpstreamBuild, PropertyQuery> pager = Rfc7233Pager
                .forRequestedRange(request.range())
                .unit("UpstreamBuild")
                .maxPageSize(100)
                .pager(this.repository);


        try {
            Rfc7233Pager.Page<UpstreamBuild> page = pager.page();
            if(! page.isValid()) {
                return this.invalidRange(request, page);
            } else {
                return this.listResponse(page);
            }
        } catch (RepositoryException e) {
            return this.repositoryException(e);
        }
    }

    private UpstreamBuildTriggersGetResponse listResponse(Rfc7233Pager.Page<UpstreamBuild> page) {
        if(page.isPartial()) {
            return UpstreamBuildTriggersGetResponse.builder()
                    .status206(status -> status
                            .payload(page.list().valueList())
                            .acceptRange(page.acceptRange())
                            .contentRange(page.contentRange())
                    )
                    .build();
        } else {
            return UpstreamBuildTriggersGetResponse.builder()
                    .status200(status -> status
                            .payload(page.list().valueList())
                            .acceptRange(page.acceptRange())
                            .contentRange(page.contentRange())
                    )
                    .build();
        }
    }

    private UpstreamBuildTriggersGetResponse invalidRange(UpstreamBuildTriggersGetRequest request, Rfc7233Pager.Page<UpstreamBuild> page) {
        return UpstreamBuildTriggersGetResponse.builder()
                .status416(status -> status.payload(error -> error
                        .token(log.audit().tokenized().error("range is invalid : {} - {}", request.range(), page.validationMessage()))
                        .code(Error.Code.ILLEGAL_RANGE_SPEC)
                        .description(page.validationMessage())
                ))
                .build();
    }

    private UpstreamBuildTriggersGetResponse repositoryException(RepositoryException e) {
        return UpstreamBuildTriggersGetResponse.builder()
                .status500(status -> status.payload(error -> error
                        .token(log.tokenized().error("repository error while browsing github btriggers", e))
                        .code(Error.Code.UNEXPECTED_ERROR)
                ))
                .build();
    }
}
