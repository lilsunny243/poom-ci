package org.codingmatters.poom.ci.pipeline.api.service.repository;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.ci.pipeline.api.service.repository.impl.FileBasedSegmentedRepository;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLogQuery;
import org.codingmatters.poom.ci.pipeline.api.service.storage.json.StageLogReader;
import org.codingmatters.poom.ci.pipeline.api.service.storage.json.StageLogWriter;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepository;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class StageLogSegmentedRepository extends FileBasedSegmentedRepository<PoomCIRepository.StageLogKey, StageLog, StageLogQuery> {
    private final JsonFactory jsonFactory;

    public StageLogSegmentedRepository(File storageDir, JsonFactory jsonFactory) throws IOException {
        super(
                storageDir,
                StageLogSegmentedRepository::createRepository,
                StageLogSegmentedRepository.marshaller(jsonFactory),
                StageLogSegmentedRepository.unmarshaller(jsonFactory)
        );
        this.jsonFactory = jsonFactory;
    }

    private static Repository<StageLog, StageLogQuery> createRepository(PoomCIRepository.StageLogKey key) {
        return new InMemoryRepository<StageLog, StageLogQuery>() {
            @Override
            public PagedEntityList<StageLog> search(StageLogQuery query, long startIndex, long endIndex) throws RepositoryException {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static ValueMarshalling<StageLog> marshaller(JsonFactory jsonFactory) {
        return value -> {
            try(ByteArrayOutputStream out = new ByteArrayOutputStream(); JsonGenerator gen = jsonFactory.createGenerator(out)) {
                new StageLogWriter().write(gen, value);
                gen.flush();
                gen.close();
                return out.toByteArray();
            }
        };
    }

    private static ValueUnmarshalling<StageLog> unmarshaller(JsonFactory jsonFactory) {
        return bytes -> {
            try(JsonParser parser = jsonFactory.createParser(bytes)) {
                return new StageLogReader().read(parser);
            }
        };
    }

}
