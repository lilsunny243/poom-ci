package org.codingmatters.poom.ci.pipeline.api.service.repository;

import org.codingmatters.poom.services.domain.repositories.Repository;

import java.io.IOException;

public interface SegmentedRepository<K extends SegmentedRepository.Key, V, Q> {
    interface Key {
        String segmentName();
    }

    @FunctionalInterface
    interface ValueMarshalling<V> {
        byte[] marshall(V value) throws IOException;
    }

    @FunctionalInterface
    interface ValueUnmarshalling<V> {
        V unmarshall(byte[] bytes) throws IOException;
    }

    Repository<V, Q> repository(K key);

}
