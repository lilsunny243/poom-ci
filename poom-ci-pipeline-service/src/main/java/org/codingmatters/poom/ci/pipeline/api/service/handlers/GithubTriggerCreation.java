package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.GithubTriggersPostRequest;
import org.codingmatters.poom.ci.pipeline.api.GithubTriggersPostResponse;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.rest.api.Processor;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GithubTriggerCreation implements Function<GithubTriggersPostRequest, GithubTriggersPostResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(GithubTriggerCreation.class);

    private final Repository<GithubPushEvent, String> repository;
    private final Consumer<PipelineTrigger> triggerCreated;

    public GithubTriggerCreation(PoomCIRepository repository, Consumer<PipelineTrigger> triggerCreated) {
        this.repository = repository.githubPushEventRepository();
        this.triggerCreated = triggerCreated;
    }

    @Override
    public GithubTriggersPostResponse apply(GithubTriggersPostRequest request) {
        try {
            Entity<GithubPushEvent> trigger = this.repository.create(request.payload());
            log.audit().info("trigger created for github push event {}", trigger);

            this.triggerCreated.accept(PipelineTrigger.builder()
                    .type(PipelineTrigger.Type.GITHUB_PUSH)
                    .triggerId(trigger.id())
                    .name(this.nameFrom(request.payload()))
                    .build());
            return GithubTriggersPostResponse.builder()
                    .status201(status -> status
                        .xEntityId(trigger.id())
                        .location(Processor.Variables.API_PATH.token() + "/triggers/git-hub/" + trigger.id())
                    )
                    .build();

        } catch (RepositoryException e) {
            return GithubTriggersPostResponse.builder().status500(status -> status.payload(error -> error
                            .token(log.tokenized().error("error while storing push event to repository", e))
                            .code(Error.Code.UNEXPECTED_ERROR)
                    ))
                    .build();
        }
    }

    private String nameFrom(GithubPushEvent event) {
        String authors = "";
        if(event.opt().commits().isPresent()) {
            authors = event.commits().stream().map(commit -> commit.author().name()).collect(Collectors.joining(","));
        }
        return String.format(
                "%s (%s-%s) triggered by push from : ",
                event.opt().repository().name().orElse("none"),
                event.ref(),
                event.after(),
                authors
        );
    }
}
