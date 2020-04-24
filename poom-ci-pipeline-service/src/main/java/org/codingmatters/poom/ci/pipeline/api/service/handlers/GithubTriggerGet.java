package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.GithubTriggerGetRequest;
import org.codingmatters.poom.ci.pipeline.api.GithubTriggerGetResponse;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;

import java.util.function.Function;

public class GithubTriggerGet implements Function<GithubTriggerGetRequest, GithubTriggerGetResponse> {
    static private CategorizedLogger log = CategorizedLogger.getLogger(GithubTriggersBrowsing.class);

    private final Repository<GithubPushEvent, PropertyQuery> repository;

    public GithubTriggerGet(PoomCIRepository repository) {
        this.repository = repository.githubPushEventRepository();
    }

    @Override
    public GithubTriggerGetResponse apply(GithubTriggerGetRequest request) {
        try {
            Entity<GithubPushEvent> trigger = this.repository.retrieve(request.triggerId());
            if(trigger != null) {
                return GithubTriggerGetResponse.builder()
                        .status200(status -> status
                                .payload(trigger.value())
                                .xEntityId(trigger.id())
                        )
                        .build();
            } else {
                return GithubTriggerGetResponse.builder()
                        .status404(status -> status.payload(error -> error
                                .token(log.audit().tokenized().info("requested for a not existing github trigger : {}", request.triggerId()))
                                .code(Error.Code.RESOURCE_NOT_FOUND)
                                .description("GithubPushEvent not found")
                        ))
                        .build();
            }
        } catch (RepositoryException e) {
            return GithubTriggerGetResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .token(log.tokenized().error("error accessing trigger repository", e))
                            .code(Error.Code.UNEXPECTED_ERROR)
                    ))
                    .build();
        }
    }
}
