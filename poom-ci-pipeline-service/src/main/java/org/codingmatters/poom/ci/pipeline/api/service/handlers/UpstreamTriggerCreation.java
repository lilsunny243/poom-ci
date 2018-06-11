package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.UpstreamBuildTriggersPostRequest;
import org.codingmatters.poom.ci.pipeline.api.UpstreamBuildTriggersPostResponse;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.triggers.UpstreamBuild;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.rest.api.Processor;

import java.util.function.Consumer;
import java.util.function.Function;

public class UpstreamTriggerCreation implements Function<UpstreamBuildTriggersPostRequest, UpstreamBuildTriggersPostResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(UpstreamTriggerCreation.class);

    private final Repository<UpstreamBuild, String> repository;
    private final Consumer<PipelineTrigger> triggerCreated;

    public UpstreamTriggerCreation(PoomCIRepository repository, Consumer<PipelineTrigger> triggerCreated) {
        this.repository = repository.upstreamBuildRepository();
        this.triggerCreated = triggerCreated;
    }

    @Override
    public UpstreamBuildTriggersPostResponse apply(UpstreamBuildTriggersPostRequest request) {
        try {
            Entity<UpstreamBuild> trigger = this.repository.create(request.payload());
            log.audit().info("trigger created for upstream build {}", trigger);

            this.triggerCreated.accept(PipelineTrigger.builder()
                    .type(PipelineTrigger.Type.UPSTREAM_BUILD)
                    .triggerId(trigger.id())
                    .build());
            return UpstreamBuildTriggersPostResponse.builder()
                    .status201(status -> status
                            .xEntityId(trigger.id())
                            .location(Processor.Variables.API_PATH.token() + "/triggers/git-hub/" + trigger.id())
                    )
                    .build();

        } catch (RepositoryException e) {
            return UpstreamBuildTriggersPostResponse.builder().status500(status -> status.payload(error -> error
                    .token(log.tokenized().error("error while storing push event to repository", e))
                    .code(Error.Code.UNEXPECTED_ERROR)
            ))
                    .build();
        }
    }
}
