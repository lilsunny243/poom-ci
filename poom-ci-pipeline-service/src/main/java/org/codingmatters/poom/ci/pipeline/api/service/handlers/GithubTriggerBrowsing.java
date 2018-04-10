package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.GithubTriggersGetRequest;
import org.codingmatters.poom.ci.pipeline.api.GithubTriggersGetResponse;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;

import java.util.function.Function;

public class GithubTriggerBrowsing implements Function<GithubTriggersGetRequest, GithubTriggersGetResponse> {
    static private CategorizedLogger log = CategorizedLogger.getLogger(GithubTriggerBrowsing.class);

    private final Repository<GithubPushEvent, String> githubPushEventRepository;

    public GithubTriggerBrowsing(Repository<GithubPushEvent, String> githubPushEventRepository) {
        this.githubPushEventRepository = githubPushEventRepository;
    }

    @Override
    public GithubTriggersGetResponse apply(GithubTriggersGetRequest request) {
        Rfc7233Pager<GithubPushEvent, String> pager = Rfc7233Pager
                .forRequestedRange(request.range())
                .unit("GithubPushEvent")
                .maxPageSize(100)
                .pager(this.githubPushEventRepository);


        try {
            Rfc7233Pager.Page<GithubPushEvent> page = pager.page();
            if(! page.isValid()) {
                return this.invalidRange(request, page);
            } else {
                return this.listResponse(page);
            }
        } catch (RepositoryException e) {
            return this.repositoryException(e);
        }
    }

    private GithubTriggersGetResponse listResponse(Rfc7233Pager.Page<GithubPushEvent> page) {
        if(page.isPartial()) {
            return GithubTriggersGetResponse.builder()
                    .status206(status -> status
                            .payload(page.list().valueList())
                            .acceptRange(page.acceptRange())
                            .contentRange(page.contentRange())
                    )
                    .build();
        } else {
            return GithubTriggersGetResponse.builder()
                    .status200(status -> status
                            .payload(page.list().valueList())
                            .acceptRange(page.acceptRange())
                            .contentRange(page.contentRange())
                    )
                    .build();
        }
    }

    private GithubTriggersGetResponse invalidRange(GithubTriggersGetRequest request, Rfc7233Pager.Page<GithubPushEvent> page) {
        return GithubTriggersGetResponse.builder()
                .status416(status -> status.payload(error -> error
                        .token(log.audit().tokenized().error("range is invalid : {} - {}", request.range(), page.validationMessage()))
                        .code(Error.Code.ILLEGAL_RANGE_SPEC)
                        .description(page.validationMessage())
                ))
                .build();
    }

    private GithubTriggersGetResponse repositoryException(RepositoryException e) {
        return GithubTriggersGetResponse.builder()
                .status500(status -> status.payload(error -> error
                        .token(log.tokenized().error("repository error while browsing github btriggers", e))
                        .code(Error.Code.UNEXPECTED_ERROR)
                ))
                .build();
    }
}
