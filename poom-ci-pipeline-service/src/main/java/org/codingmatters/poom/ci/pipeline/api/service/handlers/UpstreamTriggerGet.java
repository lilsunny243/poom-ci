package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.UpstreamBuildTriggerGetRequest;
import org.codingmatters.poom.ci.pipeline.api.UpstreamBuildTriggerGetResponse;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.UpstreamBuildQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.triggers.UpstreamBuild;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;

import java.util.function.Function;

public class UpstreamTriggerGet implements Function<UpstreamBuildTriggerGetRequest, UpstreamBuildTriggerGetResponse> {
    static private CategorizedLogger log = CategorizedLogger.getLogger(UpstreamTriggerGet.class);

    private final Repository<UpstreamBuild, UpstreamBuildQuery> repository;

    public UpstreamTriggerGet(PoomCIRepository repository) {
        this.repository = repository.upstreamBuildRepository();
    }

    @Override
    public UpstreamBuildTriggerGetResponse apply(UpstreamBuildTriggerGetRequest request) {
        try {
            Entity<UpstreamBuild> trigger = this.repository.retrieve(request.triggerId());
            if(trigger != null) {
                return UpstreamBuildTriggerGetResponse.builder()
                        .status200(status -> status
                                .payload(trigger.value())
                                .xEntityId(trigger.id())
                        )
                        .build();
            } else {
                return UpstreamBuildTriggerGetResponse.builder()
                        .status404(status -> status.payload(error -> error
                                .token(log.audit().tokenized().info("requested for a not existing github trigger : {}", request.triggerId()))
                                .code(Error.Code.RESOURCE_NOT_FOUND)
                                .description("GithubPushEvent not found")
                        ))
                        .build();
            }
        } catch (RepositoryException e) {
            return UpstreamBuildTriggerGetResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .token(log.tokenized().error("error accessing trigger repository", e))
                            .code(Error.Code.UNEXPECTED_ERROR)
                    ))
                    .build();
        }
    }
}
