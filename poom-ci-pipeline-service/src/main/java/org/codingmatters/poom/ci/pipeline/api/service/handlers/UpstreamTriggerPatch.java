package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.UpstreamBuildTriggerPatchRequest;
import org.codingmatters.poom.ci.pipeline.api.UpstreamBuildTriggerPatchResponse;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.UpstreamBuildQuery;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.triggers.UpstreamBuild;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;

import java.util.function.Function;

public class UpstreamTriggerPatch implements Function<UpstreamBuildTriggerPatchRequest, UpstreamBuildTriggerPatchResponse> {

    static private final CategorizedLogger log = CategorizedLogger.getLogger(UpstreamTriggerPatch.class);

    private final Repository<UpstreamBuild, UpstreamBuildQuery> repository;

    public UpstreamTriggerPatch(PoomCIRepository repository) {
        this.repository = repository.upstreamBuildRepository();
    }

    @Override
    public UpstreamBuildTriggerPatchResponse apply(UpstreamBuildTriggerPatchRequest request) {
        try {
            Entity<UpstreamBuild> build = this.repository.retrieve(request.triggerId());
            if(build != null) {
                Entity<UpstreamBuild> updated = this.repository.update(build, build.value().withConsumed(request.payload().consumed()));
                return UpstreamBuildTriggerPatchResponse.builder()
                        .status200( status -> status
                                .payload(updated.value())
                        )
                        .build();
            } else {
                return UpstreamBuildTriggerPatchResponse.builder()
                        .status404(status -> status.payload(
                                error -> error
                                        .code(Error.Code.RESOURCE_NOT_FOUND)
                                        .token(log.tokenized().info("no such build {}", request.triggerId()))
                                        .description("the requested upstream build trigger was not found")
                        ))
                        .build();
            }
        } catch (RepositoryException e) {
            return UpstreamBuildTriggerPatchResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .code(Error.Code.UNEXPECTED_ERROR)
                            .token(log.tokenized().error("error accessing repository", e))
                    ))
                    .build();
        }
    }
}
