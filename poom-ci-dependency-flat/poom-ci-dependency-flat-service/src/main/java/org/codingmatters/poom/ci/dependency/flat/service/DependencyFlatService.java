package org.codingmatters.poom.ci.dependency.flat.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.mongodb.client.MongoClient;
import io.flexio.io.mongo.repository.MongoCollectionRepository;
import io.flexio.io.mongo.repository.property.query.PropertyQuerier;
import io.flexio.services.support.mondo.MongoProvider;
import io.undertow.Undertow;
import org.codingmatters.poom.ci.dependency.api.PoomCIDependencyAPIDescriptor;
import org.codingmatters.poom.ci.dependency.api.processor.PoomCIDependencyAPIProcessor;
import org.codingmatters.poom.ci.dependency.api.types.Repository;
import org.codingmatters.poom.ci.dependency.api.types.mongo.RepositoryMongoMapper;
import org.codingmatters.poom.ci.dependency.flat.GraphManager;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.DependsOnRelation;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.ProducesRelation;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.mongo.DependsOnRelationMongoMapper;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.mongo.ProducesRelationMongoMapper;
import org.codingmatters.poom.ci.dependency.flat.handlers.FlatDependencyHandlersBuilder;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

public class DependencyFlatService {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(DependencyFlatService.class);

    static private final String NAME = "dependency flat service";
    private static final String DEPENDENCY_DB = "DEPENDENCY_DB";

    private final GraphManager graphManager;
    private final String host;
    private final int port;
    private final JsonFactory jsonFactory;

    private final Undertow server;

    public DependencyFlatService(GraphManager graphManager, String host, int port, JsonFactory jsonFactory) {
        this.graphManager = graphManager;
        this.host = host;
        this.port = port;
        this.jsonFactory = jsonFactory;

        this.server = Undertow.builder()
                .addHttpListener(this.port, this.host)
                .setHandler(new CdmHttpUndertowHandler(this.processor()))
                .build();
    }

    private Processor processor() {
        return new PoomCIDependencyAPIProcessor(
                "/" + PoomCIDependencyAPIDescriptor.NAME,
                this.jsonFactory,
                new FlatDependencyHandlersBuilder(this.graphManager).build()
        );
    }

    public void start() {
        log.info("starting " + NAME + "...");
        this.server.start();
        log.info(NAME + " started.");
    }

    public void stop() {
        log.info("stopping " + NAME + "...");
        this.server.stop();
        log.info(NAME + " stopped.");
    }

    public static void main(String[] args) {
        DependencyFlatService service = fromEnv();

        service.start();
        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        service.stop();
    }

    private static DependencyFlatService fromEnv() {
        MongoClient mongoClient = MongoProvider.fromEnv();
        String database = Env.mandatory(DEPENDENCY_DB).asString();
        GraphManager graphManager = new GraphManager(
                repositoriesMongoRepository(mongoClient, database),
                producesRelationRepository(mongoClient, database),
                dependsOnRelationRepository(mongoClient, database),
                1000
        );
        return new DependencyFlatService(graphManager, Env.mandatory(Env.SERVICE_HOST).asString(), Env.mandatory(Env.SERVICE_PORT).asInteger(), new JsonFactory());
    }

    public static org.codingmatters.poom.services.domain.repositories.Repository<Repository, PropertyQuery> repositoriesMongoRepository(MongoClient mongoClient, String database) {
        RepositoryMongoMapper mapper = new RepositoryMongoMapper();

        return MongoCollectionRepository.<Repository, PropertyQuery>repository(database, "repositories")
                .withToDocument(mapper::toDocument)
                .withToValue(mapper::toValue)
                .buildWithPropertyQuery(mongoClient);
    }

    public static org.codingmatters.poom.services.domain.repositories.Repository<ProducesRelation, PropertyQuery> producesRelationRepository(MongoClient mongoClient, String database) {
        ProducesRelationMongoMapper mapper = new ProducesRelationMongoMapper();

        return MongoCollectionRepository.<ProducesRelation, PropertyQuery>repository(database, "produces_relation")
                .withToDocument(mapper::toDocument)
                .withToValue(mapper::toValue)
                .buildWithPropertyQuery(mongoClient);
    }

    public static org.codingmatters.poom.services.domain.repositories.Repository<DependsOnRelation, PropertyQuery> dependsOnRelationRepository(MongoClient mongoClient, String database) {
        DependsOnRelationMongoMapper mapper = new DependsOnRelationMongoMapper();

        return MongoCollectionRepository.<DependsOnRelation, PropertyQuery>repository(database, "dependson_relation")
                .withToDocument(mapper::toDocument)
                .withToValue(mapper::toValue)
                .buildWithPropertyQuery(mongoClient);
    }


}


