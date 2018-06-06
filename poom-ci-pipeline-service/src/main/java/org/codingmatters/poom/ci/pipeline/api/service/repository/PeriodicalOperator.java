package org.codingmatters.poom.ci.pipeline.api.service.repository;

import org.codingmatters.poom.ci.pipeline.api.service.repository.impl.FileBasedSegmentedRepository;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class PeriodicalOperator<K extends SegmentedRepository.Key, V, Q> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PeriodicalOperator.class);

    public interface PeriodicalOperations {
        static <K extends SegmentedRepository.Key, V, Q> Consumer<FileBasedSegmentedRepository<K, V, Q>> storer() {
            return segmentedRepository -> {
                try {
                    segmentedRepository.store();
                } catch (IOException e) {
                    log.error("error storing repositories", e);
                }
            };
        }
        static <K extends SegmentedRepository.Key, V, Q> Consumer<FileBasedSegmentedRepository<K, V, Q>> purger(long ttl) {
            return segmentedRepository -> segmentedRepository.purge(ttl);
        }
    }

    private final FileBasedSegmentedRepository<K, V, Q> segmentedRepository;
    private final long storageDelay;
    private final TimeUnit delayUnit;

    private final Consumer<FileBasedSegmentedRepository<K, V, Q>> operation;

    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduled;

    public PeriodicalOperator(
            FileBasedSegmentedRepository<K, V, Q> segmentedRepository,
            Consumer<FileBasedSegmentedRepository<K, V, Q>> operation,
            ScheduledExecutorService scheduler,
            long storageDelay,
            TimeUnit delayUnit
    ) {
        this.segmentedRepository = segmentedRepository;
        this.operation = operation;
        this.scheduler = scheduler;
        this.storageDelay = storageDelay;
        this.delayUnit = delayUnit;
    }

    public PeriodicalOperator start() {
        if(this.scheduled == null || this.scheduled.isDone()) {
            this.scheduled = this.scheduler.scheduleAtFixedRate(() -> this.operation.accept(this.segmentedRepository), 0L, this.storageDelay, this.delayUnit);
        } else {
            log.info("periodical scheduler is already running");
        }
        return this;
    }

    public PeriodicalOperator stop() throws Exception {
        if(this.scheduled.isDone()) return this;

        this.scheduled.cancel(false);
        this.operation.accept(this.segmentedRepository);
        try {
            this.scheduled.get(this.storageDelay * 2, this.delayUnit);
        } catch(CancellationException e) {
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw e;
        }
        return this;
    }
}
