package org.codingmatters.poom.ci.pipeline.api.service.repository.impl;

import org.codingmatters.poom.ci.pipeline.api.service.repository.SegmentedRepository;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepository;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.io.*;
import java.util.Objects;
import java.util.function.Function;

public class AbstractBasedSegmentedRepositoryTest {

    protected String content(File storageFile) throws IOException {
        try(Reader reader = new FileReader(storageFile)) {
            StringBuilder result = new StringBuilder();
            char [] buffer = new char[1024];
            for(int read = reader.read(buffer) ; read != -1 ; read = reader.read(buffer)) {
                result.append(buffer, 0, read);
            }
            return result.toString();
        }
    }

    protected void contentToFile(String content, File to) throws IOException {
        try(FileWriter writer = new FileWriter(to)) {
            writer.write(content);
            writer.flush();
        }
    }

    static protected FileBasedSegmentedRepository<TestKey, String, String> createTestSegmentedRepository(File storageDir) throws IOException {
        return new FileBasedSegmentedRepository<>(storageDir, createRepository, marshaller, unmarshaller);
    }

    static private Function<TestKey, Repository<String, String>> createRepository = key -> new TestRepository();
    static private SegmentedRepository.ValueMarshalling<String> marshaller = value -> value.getBytes();
    static private SegmentedRepository.ValueUnmarshalling<String> unmarshaller = bytes -> bytes != null ? new String(bytes) : null;

    static class TestRepository extends InMemoryRepository<String, String> {
        @Override
        public PagedEntityList<String> search(String query, long startIndex, long endIndex) throws RepositoryException {
            throw new UnsupportedOperationException();
        }
    }

    static class TestKey implements SegmentedRepository.Key {
        private final String segment;

        TestKey(String segment) {
            this.segment = segment;
        }

        @Override
        public String segmentName() {
            return this.segment;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestKey testKey = (TestKey) o;
            return Objects.equals(segment, testKey.segment);
        }

        @Override
        public int hashCode() {
            return Objects.hash(segment);
        }
    }
}
