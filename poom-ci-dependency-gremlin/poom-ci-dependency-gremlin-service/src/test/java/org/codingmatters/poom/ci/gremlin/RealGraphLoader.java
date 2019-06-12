package org.codingmatters.poom.ci.gremlin;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.ci.dependency.api.RepositoryPutRequest;
import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.ValueList;
import org.codingmatters.poom.ci.dependency.api.types.json.FullRepositoryReader;
import org.codingmatters.poom.ci.dependency.api.types.json.ModuleReader;
import org.codingmatters.poom.ci.gremlin.service.handlers.CreateOrUpdateRepository;

import java.io.IOException;

public class RealGraphLoader {
    static private final JsonFactory jsonFactory = new JsonFactory();

    static public void load(String sample, GremlinResource gremlin) throws Exception {
        FullRepository[] repositories = readRepos(sample + "/repositories.json");
        for (int i = 0; i < repositories.length; i++) {
            FullRepository repository = repositories[i];
            repository = repository.withDependencies(new ValueList.Builder<>().with(readModules(sample + "/depends-on/" + repository.id() + ".json")).build());
            repository = repository.withProduces(new ValueList.Builder<>().with(readModules(sample + "/produces/" + repository.id() + ".json")).build());
            repositories[i] = repository;
        }
        for (FullRepository repository : repositories) {
            new CreateOrUpdateRepository(gremlin.remoteConnection()).apply(RepositoryPutRequest.builder()
                    .repositoryId(repository.id())
                    .payload(repository)
                    .build()).opt().status200().orElseThrow(() -> new AssertionError("failed to create repository " + repository));
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
