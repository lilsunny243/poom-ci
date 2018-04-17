package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.GithubTriggersPostRequest;
import org.codingmatters.poom.ci.pipeline.api.GithubTriggersPostResponse;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.rest.api.Processor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Function;

public class GithubTriggerCreation implements Function<GithubTriggersPostRequest, GithubTriggersPostResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(GithubTriggerCreation.class);

    private final Repository<GithubPushEvent, String> githubPushEventRepository;
    private final Repository<Pipeline, String> pipelineRepository;

    public GithubTriggerCreation(PoomCIRepository repository) {
        this.githubPushEventRepository = repository.githubPushEventRepository();
        this.pipelineRepository = repository.pipelineRepository();
    }

    @Override
    public GithubTriggersPostResponse apply(GithubTriggersPostRequest request) {
        return new Creation(
                this.githubPushEventRepository,
                this.pipelineRepository,
                request.payload()
        ).create();
    }

    class Creation {
        private final Repository<GithubPushEvent, String> githubPushEventRepository;
        private final Repository<Pipeline, String> pipelineRepository;
        private final GithubPushEvent event;
        private final GithubTriggersPostResponse.Builder response = GithubTriggersPostResponse.builder();

        Creation(Repository<GithubPushEvent, String> githubPushEventRepository, Repository<Pipeline, String> pipelineRepository, GithubPushEvent event) {
            this.githubPushEventRepository = githubPushEventRepository;
            this.pipelineRepository = pipelineRepository;
            this.event = event;
        }

        GithubTriggersPostResponse create() {
            Optional<Entity<GithubPushEvent>> trigger = this.createTrigger();
            if(trigger.isPresent()) {
                Optional<Entity<Pipeline>> pipeline = this.createPipeline(trigger.get());
                if(pipeline.isPresent()) {
                    this.response.status201(status -> status
                            .xEntityId(trigger.get().id())
                            .location(Processor.Variables.API_PATH.token() + "/triggers/git-hub/" + trigger.get().id())
                    );
                }
            }

            return this.response.build();
        }

        private Optional<Entity<GithubPushEvent>> createTrigger() {
            Entity<GithubPushEvent> trigger;
            try {
                trigger = this.githubPushEventRepository.create(this.event);
                log.audit().info("trigger created for github push event {}", trigger);
                return Optional.of(trigger);
            } catch (RepositoryException e) {
                this.response
                        .status500(status -> status.payload(error -> error
                                .token(log.tokenized().error("error while storing push event to repository", e))
                                .code(Error.Code.UNEXPECTED_ERROR)
                        ));
                return Optional.empty();
            }
        }

        private Optional<Entity<Pipeline>> createPipeline(Entity<GithubPushEvent> trigger) {
            Entity<Pipeline> pipeline;
            try {
                pipeline = this.pipelineRepository.create(Pipeline.builder()
                        .status(status -> status
                                .run(Status.Run.RUNNING)
                                .exit(null)
                                .triggered(LocalDateTime.now(ZoneOffset.UTC.normalized()))
                        )
                        .trigger(trig -> trig
                                .type(PipelineTrigger.Type.GITHUB_PUSH)
                                .triggerId(trigger.id())
                        )
                        .build());


                pipeline = this.pipelineRepository.update(pipeline, pipeline.value().withId(pipeline.id()));

                log.audit().info("pipeline created for trigger {}", pipeline);
                return Optional.of(pipeline);
            } catch (RepositoryException e) {
                this.response
                        .status500(status -> status.payload(error -> error
                                .token(log.tokenized().error("error while storing pipeline to repository", e))
                                .code(Error.Code.UNEXPECTED_ERROR)
                        ));
                return Optional.empty();
            }
        }


    }
}
