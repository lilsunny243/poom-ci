package org.codingmatters.poom.ci.gremlin;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.codingmatters.poom.ci.dependency.api.RepositoryPutRequest;
import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.ValueList;
import org.codingmatters.poom.ci.dependency.api.types.json.FullRepositoryReader;
import org.codingmatters.poom.ci.dependency.api.types.json.ModuleReader;
import org.codingmatters.poom.ci.gremlin.service.GremlinDependencyService;
import org.codingmatters.poom.ci.gremlin.service.handlers.CreateOrUpdateRepository;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.io.IOException;
import java.util.function.Supplier;

public class RealGraphLoader {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RealGraphLoader.class);

    static private final JsonFactory jsonFactory = new JsonFactory();

    public static void main(String[] args) {
        Supplier<RemoteConnection> connectionSupplier = GremlinDependencyService.createConnectionSupplier();
        try {
            load("real-sample-2019-06-03", connectionSupplier);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static public void load(String sample, GremlinResource gremlin) throws Exception {
        Supplier<RemoteConnection> connectionSupplier = gremlin.remoteConnectionSupplier();
        load(sample, connectionSupplier);
    }

    public static void load(String sample, Supplier<RemoteConnection> connectionSupplier) throws IOException {
        FullRepository[] repositories = readRepos(sample + "/repositories.json");
        for (int i = 0; i < repositories.length; i++) {
            FullRepository repository = repositories[i];
            repository = repository.withDependencies(new ValueList.Builder<>().with(readModules(sample + "/depends-on/" + repository.id() + ".json")).build());
            repository = repository.withProduces(new ValueList.Builder<>().with(readModules(sample + "/produces/" + repository.id() + ".json")).build());
            repositories[i] = repository;
        }
        for (FullRepository repository : repositories) {
            long start = System.currentTimeMillis();
            new CreateOrUpdateRepository(connectionSupplier).apply(RepositoryPutRequest.builder()
                    .repositoryId(repository.id())
                    .payload(repository)
                    .build()).opt().status200().orElseThrow(() -> new AssertionError("failed to create repository " + repository));
            log.info("repo {} : {}ms", repository.id(), (System.currentTimeMillis() - start));
        }
    }

    private static FullRepository[] readRepos(String resource) throws IOException {
        try(JsonParser parser = jsonFactory.createParser(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))) {
            return new FullRepositoryReader().readArray(parser);
        }
    }

    private static Module[] readModules(String resource) throws IOException {
        try(JsonParser parser = jsonFactory.createParser(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))) {
            return new ModuleReader().readArray(parser);
        } catch (IOException e) {
            return new Module[0];
        }
    }
}
