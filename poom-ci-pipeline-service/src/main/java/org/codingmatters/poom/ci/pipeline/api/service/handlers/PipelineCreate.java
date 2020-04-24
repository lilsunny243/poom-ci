package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.PipelinesPostRequest;
import org.codingmatters.poom.ci.pipeline.api.PipelinesPostResponse;
import org.codingmatters.poom.ci.pipeline.api.pipelinespostresponse.Status200;
import org.codingmatters.poom.ci.pipeline.api.pipelinespostresponse.Status201;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.rest.api.Processor;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class PipelineCreate implements Function<PipelinesPostRequest, PipelinesPostResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PipelineCreate.class);

    private final Repository<Pipeline, PropertyQuery> pipelineRepository;
    private final Consumer<Pipeline> pipelineCreationListener;

    public PipelineCreate(PoomCIRepository repository, Consumer<Pipeline> pipelineCreationListener) {
        this.pipelineRepository = repository.pipelineRepository();
        this.pipelineCreationListener = pipelineCreationListener;
    }

    @Override
    public PipelinesPostResponse apply(PipelinesPostRequest request) {
        try {
            Optional<PipelinesPostResponse> invalid = this.validate(request);
            if(invalid.isPresent()) return invalid.get();

            Optional<PipelinesPostResponse> alreadyPending = this.ifAlreadyPending(request);
            if(alreadyPending.isPresent()) return alreadyPending.get();

            Entity<Pipeline> entity = this.pipelineRepository.create(Pipeline.builder()
                    .trigger(request.payload())
                    .status(Status.builder()
                            .run(Status.Run.PENDING)
                            .triggered(UTC.now())
                            .build())
                    .name(request.payload().name())
                    .build());
            entity = this.pipelineRepository.update(entity, entity.value().withId(entity.id()));

            log.audit().info("pipeline {} created for trigger {}", entity.id(), request.payload());
            this.pipelineCreationListener.accept(entity.value());
            
            return PipelinesPostResponse.builder()
                    .status201(Status201.builder()
                            .xEntityId(entity.id())
                            .location(String.format("%s/pipelines/%s",
                                    Processor.Variables.API_PATH.token(),
                                    entity.id()))
                            .build()
                    )
                    .build();
        } catch (RepositoryException e) {
            return PipelinesPostResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .token(log.tokenized().error("error accessing pipeline repository", e))
                            .code(Error.Code.UNEXPECTED_ERROR)
                    ))
                    .build();
        }
    }

    private Optional<PipelinesPostResponse> ifAlreadyPending(PipelinesPostRequest request) throws RepositoryException {
        PropertyQuery.Builder query = PropertyQuery.builder()
                .filter(String.format("trigger.name == '%s' && status.run == '%s'",
                        request.payload().name(),
                        Status.Run.PENDING.name()
                ));

        PagedEntityList<Pipeline> exists = this.pipelineRepository.search(
//                PipelineQuery.builder()
//                .triggerName(request.payload().name())
//                .triggerRunStatus(Status.Run.PENDING.name()).build()
                query.build() , 0, 0);
        if(exists.total() == 0) {
            return Optional.empty();
        } else {
            Entity<Pipeline> alreadyPendingPipeline = exists.get(0);
            log.info("will not create pipeline as the same one is already pending : {}", alreadyPendingPipeline);
            return  Optional.of(PipelinesPostResponse.builder()
                    .status200(Status200.builder()
                            .xEntityId(alreadyPendingPipeline.id())
                            .location(String.format("%s/pipelines/%s",
                                    Processor.Variables.API_PATH.token(),
                                    alreadyPendingPipeline.id()))
                            .build())
                    .build());
        }
    }

    private Optional<PipelinesPostResponse> validate(PipelinesPostRequest request) {
        if(! request.opt().payload().isPresent()) {
            return Optional.of(PipelinesPostResponse.builder()
                    .status400(status -> status.payload(error -> error
                            .token(log.audit().tokenized().info("request creation of a pipeline with no trigger : {}", request.payload()))
                            .code(Error.Code.ILLEGAL_RESOURCE_CREATION)
                            .description("cannot create a pipeline without a trigger")
                    ))
                    .build());
        }
        if(! request.opt().payload().type().isPresent()) {
            return Optional.of(PipelinesPostResponse.builder()
                    .status400(status -> status.payload(error -> error
                            .token(log.audit().tokenized().info("request creation of a pipeline with no trigger type {}", request.payload()))
                            .code(Error.Code.ILLEGAL_RESOURCE_CREATION)
                            .description("cannot create a pipeline without a trigger type")
                    ))
                    .build());
        }
        if(! request.opt().payload().triggerId().isPresent()) {
            return Optional.of(PipelinesPostResponse.builder()
                    .status400(status -> status.payload(error -> error
                            .token(log.audit().tokenized().info("request creation of a pipeline with no trigger id {}", request.payload()))
                            .code(Error.Code.ILLEGAL_RESOURCE_CREATION)
                            .description("cannot create a pipeline without a trigger id")
                    ))
                    .build());
        }
        if(! request.opt().payload().name().isPresent()) {
            return Optional.of(PipelinesPostResponse.builder()
                    .status400(status -> status.payload(error -> error
                            .token(log.audit().tokenized().info("request creation of a pipeline with no trigger name {}", request.payload()))
                            .code(Error.Code.ILLEGAL_RESOURCE_CREATION)
                            .description("cannot create a pipeline without a trigger name")
                    ))
                    .build());
        }
        return Optional.empty();
    }
}
