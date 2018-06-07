package org.codingmatters.poom.ci.pipeline.api.service.repository.impl;

import org.codingmatters.poom.ci.pipeline.api.service.repository.SegmentedRepository;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.io.*;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class FileBasedSegmentedRepository<K extends SegmentedRepository.Key, V, Q> implements SegmentedRepository<K, V, Q> {

    static private final CategorizedLogger log = CategorizedLogger.getLogger(FileBasedSegmentedRepository.class);

    static public <K extends SegmentedRepository.Key> String storageFilename(K key) {
        return key.segmentName() + "-repository.storage";
    }


    private final File storageDir;
    private final Function<K, Repository<V, Q>> createRepository;
    private final ValueMarshalling<V> marshaller;
    private final ValueUnmarshalling<V> unmarshaller;

    private final Map<K, RepositoryWrapper<V, Q>> loaded = new HashMap<>();

    public FileBasedSegmentedRepository(File storageDir, Function<K, Repository<V, Q>> createRepository, ValueMarshalling<V> marshaller, ValueUnmarshalling<V> unmarshaller) throws IOException {
        this.storageDir = storageDir;
        this.createRepository = createRepository;
        this.marshaller = marshaller;
        this.unmarshaller = unmarshaller;
        if(! storageDir.exists()) {
            storageDir.mkdirs();
        }
        if(! storageDir.isDirectory()) {
            throw new IOException("storage dir must be a directory");
        }
    }

    @Override
    public Repository<V, Q> repository(K key) {
        synchronized (this.loaded) {
            RepositoryWrapper<V, Q> result = this.loaded.computeIfAbsent(key, this::load);
            result.accessed();
            return result;
        }
    }

    public int loadedRepositoryCount() {
        synchronized (this.loaded) {
            return this.loaded.size();
        }
    }

    private RepositoryWrapper<V, Q> load(K key) {
        RepositoryWrapper<V, Q> repository = new RepositoryWrapper<>(this.createRepository.apply(key));
        try {
            if (this.storageFile(key).exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(this.storageFile(key)))) {
                    for(String id = reader.readLine() ; id != null ; id = reader.readLine()) {
                        String version = reader.readLine();
                        String value = reader.readLine();
                        repository.createWithIdAndVersion(id, new BigInteger(version), this.unmarshaller.unmarshall(value != null ? value.getBytes() : null));
                    }
                }
            }
        } catch (IOException e) {
            log.error("error loading repository " + key + " from " + this.storageFile(key), e);
        } catch (RepositoryException e) {
            log.error("error loading repository " + key + " content from " + this.storageFile(key), e);
        }
        return repository;
    }

    public void store() throws IOException {
        for (Map.Entry<K, RepositoryWrapper<V, Q>> entry : this.loadedEntries()) {
            try {
                entry.getValue().ifChanged((repository, changed) -> {
                    this.storeRepository(entry.getKey(), repository);
                    changed.set(false);
                });
            } catch (Exception e) {
                throw new IOException("error storing atomically repository " + entry.getKey(), e);
            }
        }
    }

    private LinkedList<Map.Entry<K, RepositoryWrapper<V, Q>>> loadedEntries() {
        synchronized (this.loaded) {
            return new LinkedList<>(this.loaded.entrySet());
        }
    }

    private void storeRepository(K key, Repository<V, Q> repository) throws IOException {
        File storageFile = this.temporaryStorageFile(key);
        if(! storageFile.exists()) {
            storageFile.createNewFile();


            try(OutputStream out = new FileOutputStream(storageFile)) {
                long start = 0;
                long step = 1000;

                long total;
                try {
                    total = repository.all(0, 0).total();
                } catch (RepositoryException e) {
                    throw new IOException("failed calculating total from repository", e);
                }
                long stored = 0;

                PagedEntityList<V> page;
                do {
                    long end = start + step - 1;
                    try {
                        page = repository.all(start, end);
                    } catch (RepositoryException e) {
                        throw new IOException("failed getting values from repository", e);
                    }
                    for (Entity<V> entity : page) {
                        this.storeEntity(entity, out);
                        stored++;
                    }

                } while (stored < total);
            }

            boolean renamed = storageFile.renameTo(this.storageFile(key));
            if(! renamed) {
                throw new IOException("failed swapping temporary file : " + storageFile.getAbsolutePath());
            }
        }

    }

    private File temporaryStorageFile(K key) {
        return new File(this.storageDir, storageFilename(key) + ".temp");
    }

    private File storageFile(K key) {
        return new File(this.storageDir, storageFilename(key));
    }

    private void storeEntity(Entity<V> entity, OutputStream out) throws IOException {
        out.write(String.format("%s\n%s\n", entity.id(), entity.version()).getBytes());
        out.write(this.marshaller.marshall(entity.value()));
        out.write("\n".getBytes());
    }

    public void purge(long ttl) {
        long purgeTime = System.currentTimeMillis();
        Collection<K> keysToRemove = new LinkedList<>();

        for (Map.Entry<K, RepositoryWrapper<V, Q>> entry : this.loadedEntries()) {
            if(purgeTime - entry.getValue().lastAccess() > ttl) {
                if(! entry.getValue().changed()) {
                    keysToRemove.add(entry.getKey());
                }
            }
        }

        this.removeLoaded(keysToRemove);
    }

    private void removeLoaded(Collection<K> keysToRemove) {
        synchronized (this.loaded) {
            for (K k : keysToRemove) {
                this.loaded.remove(k);
            }
        }
    }

    @FunctionalInterface
    private interface RepositoryTransaction<V, Q> {
        void atomically(Repository<V, Q> repository, AtomicBoolean changed) throws Exception;
    }

    private class RepositoryWrapper<V, Q> implements Repository<V, Q>{
        private final Repository<V, Q> wrapped;
        private final AtomicLong lastAccess;
        private final AtomicBoolean changed = new AtomicBoolean(false);

        private RepositoryWrapper(Repository<V, Q> wrapped) {
            this.wrapped = wrapped;
            this.lastAccess = new AtomicLong(System.currentTimeMillis());
        }

        public void ifChanged(RepositoryTransaction<V, Q> tx) throws Exception {
            synchronized (this.changed) {
                if (this.changed.get()) {
                    tx.atomically(this.wrapped, this.changed);
                }
            }
        }

        public boolean changed() {
            return this.changed.get();
        }

        public Entity<V> createWithId(String id, V withValue) throws RepositoryException {
            synchronized (this.changed) {
                try {
                    return wrapped.createWithId(id, withValue);
                } finally {
                    this.changed.set(true);
                    this.accessed();
                }
            }
        }

        @Override
        public Entity<V> createWithIdAndVersion(String id, BigInteger bigInteger, V withValue) throws RepositoryException {
            synchronized (this.changed) {
                try {
                    return wrapped.createWithIdAndVersion(id, bigInteger, withValue);
                } finally {
                    this.changed.set(true);
                    this.accessed();
                }
            }
        }

        public Entity<V> create(V withValue) throws RepositoryException {
            synchronized (this.changed) {
                try {
                    return wrapped.create(withValue);
                } finally {
                    this.changed.set(true);
                    this.accessed();
                }
            }
        }

        public Entity<V> update(Entity<V> entity, V withValue) throws RepositoryException {
            synchronized (this.changed) {
                try {
                    return wrapped.update(entity, withValue);
                } finally {
                    this.changed.set(true);
                    this.accessed();
                }
            }
        }

        public void delete(Entity<V> entity) throws RepositoryException {
            synchronized (this.changed) {
                try {
                    wrapped.delete(entity);
                } finally {
                    this.changed.set(true);
                    this.accessed();
                }
            }
        }

        public Entity<V> retrieve(String id) throws RepositoryException {
            try {
                return wrapped.retrieve(id);
            } finally {
                this.accessed();
            }
        }

        public PagedEntityList<V> all(long startIndex, long endIndex) throws RepositoryException {
            try {
                return wrapped.all(startIndex, endIndex);
            } finally {
                this.accessed();
            }
        }

        public PagedEntityList<V> search(Q query, long startIndex, long endIndex) throws RepositoryException {
            try {
                return wrapped.search(query, startIndex, endIndex);
            } finally {
                this.accessed();
            }
        }

        public void accessed() {
            this.lastAccess.set(System.currentTimeMillis());
        }

        public long lastAccess() {
            return this.lastAccess.get();
        }
    }
}
