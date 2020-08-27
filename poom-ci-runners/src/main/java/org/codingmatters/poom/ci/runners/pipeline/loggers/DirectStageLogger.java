package org.codingmatters.poom.ci.runners.pipeline.loggers;

import org.codingmatters.poom.ci.pipeline.api.types.AppendedLogLine;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.descriptors.StageHolder;
import org.codingmatters.poom.ci.runners.pipeline.PipelineExecutor;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DirectStageLogger implements PipelineExecutor.StageLogListener, AutoCloseable {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(DirectStageLogger.class);
    public static final int LOB_BATCH_SIZE = 100;

    private final String pipilineId;
    private final StageHolder stage;
    private final PoomCIPipelineAPIClient pipelineAPIClient;
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    private final BlockingQueue<String> lineQueue = new ArrayBlockingQueue<>(2048);
    private final AtomicBoolean stop = new AtomicBoolean(false);

    public DirectStageLogger(String pipilineId, StageHolder stage, PoomCIPipelineAPIClient pipelineAPIClient) {
        this.pipilineId = pipilineId;
        this.stage = stage;
        this.pipelineAPIClient = pipelineAPIClient;
        this.pool.submit(this::processLogs);
    }

    private void processLogs() {
        while(! this.stop.get()) {
            try {
                List<String> lines = new ArrayList<>(LOB_BATCH_SIZE);
                String log = null;
                do {
                    log = this.lineQueue.poll(200, TimeUnit.MILLISECONDS);
                    if(log != null) {
                        lines.add(log);
                    }
                } while (log != null && lines.size() < LOB_BATCH_SIZE);

                if(! lines.isEmpty()) {
                    this.sendLogs(lines);
                }
            } catch (InterruptedException e) {
                log.error("error processing logs", e);
            }
        }
    }

    private void sendLogs(List<String> lines) {
        try {
            this.pipelineAPIClient.pipelines().pipeline().pipelineStages().pipelineStage().pipelineStageLogs().patch(request -> request
                    .pipelineId(this.pipilineId)
                    .stageType(this.stage.type().name())
                    .stageName(this.stage.stage().name())
                    .payload(lines.stream().map(line -> AppendedLogLine.builder().content(line).build())
                            .collect(Collectors.toList())
                    )
            );
        } catch (IOException e) {
            log.error(String.format(
                    "failed pushing pipeline %s stage %s logs",
                    this.pipilineId, this.stage),
                    e);
            log.personalData().tokenized().info("missed log for pipeline {} stage {} : {}",
                    this.pipilineId, this.stage, lines);
        }
    }

    @Override
    public void logLine(String logLine) {
        try {
            this.lineQueue.offer(logLine, 500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("failed queuing log line");
        }
    }

    @Override
    public void close() throws Exception {
        long purgeStart = System.currentTimeMillis();
        while((System.currentTimeMillis() - purgeStart < 2000) && ! this.lineQueue.isEmpty()) {
            Thread.sleep(500);
        }
        this.stop.set(true);
        this.pool.shutdown();

        this.pool.awaitTermination(2, TimeUnit.MINUTES);
        if(! this.pool.isTerminated()) {
            log.error("error sending remainig logs");
        }
    }
}
