package org.codingmatters.poom.ci.pipeline.api.service.repository.logs;

import org.codingmatters.poom.ci.pipeline.api.service.repository.LogStore;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLogQuery;
import org.codingmatters.poom.ci.pipeline.api.types.LogLine;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RepositoryLogStore implements LogStore, AutoCloseable {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RepositoryLogStore.class);

    private final Repository<StageLog, PropertyQuery> repository;

    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private final BlockingQueue<AppendRequest> appendRequestQueue = new ArrayBlockingQueue<>(10000);
    private final AtomicBoolean stop = new AtomicBoolean(false);

    public RepositoryLogStore(Repository<StageLog, PropertyQuery> repository) {
        this.repository = repository;
        this.pool.submit(this::processAppendRequests);
    }

    private void processAppendRequests() {
        while(! this.stop.get()) {
            try {
                AppendRequest request = this.appendRequestQueue.poll(500, TimeUnit.MILLISECONDS);
                if(request != null) {
                    request.appendTo(this.repository);
                }
            } catch (InterruptedException e) {
                log.error("error waiting for pending requests", e);
            } catch (IOException e) {
                log.error("error processing pending requests", e);
            }
        }
    }

    @Override
    public Segment segment(String pipelineId, Stage.StageType stageType, String stageName) {
        return new RepositorySegment(this.appendRequestQueue, this.repository, pipelineId, stageType, stageName);
    }

    @Override
    public void close() throws Exception {
        this.stop.set(true);
        this.pool.shutdown();
    }

    class RepositorySegment implements Segment {

        private final BlockingQueue<AppendRequest> appendRequestQueue;
        private final Repository<StageLog, PropertyQuery> repository;
        private final String pipelineId;
        private final Stage.StageType stageType;
        private final String stageName;

        public RepositorySegment(BlockingQueue<AppendRequest> appendRequestQueue, Repository<StageLog, PropertyQuery> repository, String pipelineId, Stage.StageType stageType, String stageName) {
            this.appendRequestQueue = appendRequestQueue;
            this.repository = repository;
            this.pipelineId = pipelineId;
            this.stageType = stageType;
            this.stageName = stageName;
        }

        @Override
        public void append(String... lines) throws IOException {
            try {
                this.appendRequestQueue.offer(
                        new AppendRequest(this.pipelineId, this.stageType, this.stageName, lines),
                        10, TimeUnit.SECONDS
                );
            } catch (InterruptedException e) {
                log.error("couldn't process log appending request, logs are lost", e);
            }
        }

        @Override
        public PagedEntityList<StageLog> all(long startIndex, long endIndex) throws RepositoryException {
            return this.repository.search(PropertyQuery.builder()
                            .filter(this.segmentFilter())
                            .sort("log.line asc")
                            .build(),
                    startIndex, endIndex);
        }

        @Override
        public PagedEntityList<StageLog> search(StageLogQuery query, long startIndex, long endIndex) throws RepositoryException {
            return this.repository.search(PropertyQuery.builder()
                    .filter(this.segmentFilter())
                    .sort("log.line asc")
                    .build(),
                    startIndex, endIndex);
        }

        public String segmentFilter() {
            return String.format(
                    "pipelineId == '%s' && stageName == '%s' && stageType == '%s'",
                    this.pipelineId,
                    this.stageName,
                    this.stageType
            );
        }
    }
}
