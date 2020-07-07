package org.codingmatters.poom.ci.pipeline.api.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.mongodb.MongoClient;
import io.flexio.io.mongo.repository.MongoCollectionRepository;
import io.flexio.io.mongo.repository.property.query.PropertyQuerier;
import io.flexio.services.support.mondo.MongoProvider;
import io.undertow.Undertow;
import org.codingmatters.poom.ci.pipeline.api.service.repository.LogStore;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.service.repository.logs.RepositoryLogStore;
import org.codingmatters.poom.ci.pipeline.api.service.storage.PipelineStage;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.service.storage.mongo.PipelineStageMongoMapper;
import org.codingmatters.poom.ci.pipeline.api.service.storage.mongo.StageLogMongoMapper;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.mongo.PipelineMongoMapper;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.ci.triggers.UpstreamBuild;
import org.codingmatters.poom.ci.triggers.mongo.GithubPushEventMongoMapper;
import org.codingmatters.poom.ci.triggers.mongo.UpstreamBuildMongoMapper;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIRequesterClient;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

public class PoomCIPipelineService {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PoomCIPipelineService.class);
    private static final String PIPELINES_DB = "PIPELINES_DB";

    public static void main(String[] args) {
        String host = Env.mandatory(Env.SERVICE_HOST).asString();
        int port = Env.mandatory(Env.SERVICE_PORT).asInteger();

        MongoClient mongoClient = MongoProvider.fromEnv();
        String database = Env.mandatory(PIPELINES_DB).asString();

        try(RepositoryLogStore logStore = new RepositoryLogStore(stagelogRepository(mongoClient, database))) {

            PoomCIPipelineService service = new PoomCIPipelineService(api(logStore, mongoClient, database), port, host);
            service.start();

            log.info("poom-ci pipeline api service running");
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            log.info("poom-ci pipeline api service stopping...");
            service.stop();
        } catch (Exception e) {
            log.error("error terminating log store", e);
        }
        log.info("poom-ci pipeline api service stopped.");
    }

    static public PoomCIApi api(LogStore logStore, MongoClient mongoClient, String database) {
        JsonFactory jsonFactory = new JsonFactory();

        PoomCIRepository repository = new PoomCIRepository(
                logStore,
                pipelineRepository(mongoClient, database),
                githubPushEventRepository(mongoClient, database),
                upstreamBuildRepository(mongoClient, database),
                pipelineStageRepository(mongoClient, database)
        );
        String jobRegistryUrl = Env.mandatory("JOB_REGISTRY_URL").asString();
        return new PoomCIApi(repository, "/pipelines", jsonFactory, new PoomjobsJobRegistryAPIRequesterClient(
                new OkHttpRequesterFactory(OkHttpClientWrapper.build(), () -> jobRegistryUrl), jsonFactory, jobRegistryUrl)
        );
    }

    private final PoomCIApi api;

    private Undertow server;
    private final int port;
    private final String host;

    public PoomCIPipelineService(PoomCIApi api, int port, String host) {
        this.port = port;
        this.host = host;
        this.api = api;
    }

    public void start() {
        this.server = Undertow.builder()
                .addHttpListener(this.port, this.host)
                .setHandler(new CdmHttpUndertowHandler(this.api.processor()))
                .build();
        this.server.start();
    }

    public void stop() {
        this.server.stop();
    }


    public static Repository<Pipeline, PropertyQuery> pipelineRepository(MongoClient mongoClient, String database) {
        PipelineMongoMapper mapper = new PipelineMongoMapper();
        PropertyQuerier querier = new PropertyQuerier();

        return MongoCollectionRepository.<Pipeline, PropertyQuery>repository(database, "pipelines")
                .withToDocument(mapper::toDocument)
                .withToValue(mapper::toValue)
                .withFilter(querier.filterer())
                .withSort(querier.sorter())
                .build(mongoClient);
    }

    public static Repository<GithubPushEvent, PropertyQuery> githubPushEventRepository(MongoClient mongoClient, String database) {
        GithubPushEventMongoMapper mapper = new GithubPushEventMongoMapper();
        PropertyQuerier querier = new PropertyQuerier();

        return MongoCollectionRepository.<GithubPushEvent, PropertyQuery>repository(database, "githib_push_events")
                .withToDocument(mapper::toDocument)
                .withToValue(mapper::toValue)
                .withFilter(querier.filterer())
                .withSort(querier.sorter())
                .build(mongoClient);
    }

    public static Repository<UpstreamBuild, PropertyQuery> upstreamBuildRepository(MongoClient mongoClient, String database) {
        UpstreamBuildMongoMapper mapper = new UpstreamBuildMongoMapper();
        PropertyQuerier querier = new PropertyQuerier();

        return MongoCollectionRepository.<UpstreamBuild, PropertyQuery>repository(database, "upstream_builds")
                .withToDocument(mapper::toDocument)
                .withToValue(mapper::toValue)
                .withFilter(querier.filterer())
                .withSort(querier.sorter())
                .build(mongoClient);
    }


    public static Repository<PipelineStage, PropertyQuery> pipelineStageRepository(MongoClient mongoClient, String database) {
        PipelineStageMongoMapper mapper = new PipelineStageMongoMapper();
        PropertyQuerier querier = new PropertyQuerier();

        return MongoCollectionRepository.<PipelineStage, PropertyQuery>repository(database, "pipeline_stages")
                .withToDocument(mapper::toDocument)
                .withToValue(mapper::toValue)
                .withFilter(querier.filterer())
                .withSort(querier.sorter())
                .build(mongoClient);
    }

    private static Repository<StageLog, PropertyQuery> stagelogRepository(MongoClient mongoClient, String database) {
        StageLogMongoMapper mapper = new StageLogMongoMapper();
        PropertyQuerier querier = new PropertyQuerier();

        return MongoCollectionRepository.<StageLog, PropertyQuery>repository(database, "stage_logs")
                .withToDocument(mapper::toDocument)
                .withToValue(mapper::toValue)
                .withFilter(querier.filterer())
                .withSort(querier.sorter())
                .build(mongoClient);
    }
}
