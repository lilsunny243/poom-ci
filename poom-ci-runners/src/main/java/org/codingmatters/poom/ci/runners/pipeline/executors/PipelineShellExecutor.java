package org.codingmatters.poom.ci.runners.pipeline.executors;

import org.codingmatters.poom.ci.pipeline.PipelineScript;
import org.codingmatters.poom.ci.pipeline.api.types.StageTermination;
import org.codingmatters.poom.ci.pipeline.descriptors.Stage;
import org.codingmatters.poom.ci.pipeline.descriptors.StageHolder;
import org.codingmatters.poom.ci.pipeline.descriptors.ValueList;
import org.codingmatters.poom.ci.runners.pipeline.PipelineContext;
import org.codingmatters.poom.ci.runners.pipeline.PipelineExecutor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.process.ProcessInvoker;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class PipelineShellExecutor implements PipelineExecutor {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PipelineShellExecutor.class);

    private final PipelineContext context;
    private final PipelineScript pipelineScript;

    public PipelineShellExecutor(PipelineContext context) {
        this.context = context;
        this.pipelineScript = new PipelineScript(this.context.pipeline());
    }

    @Override
    public void initialize() throws IOException {
    }

    @Override
    public StageTermination.Exit execute(StageHolder stage, StageLogListener logListener) throws IOException {
        this.ensureStageExists(stage);

        File stageScript = this.createStageScript(stage);
        stageScript.setExecutable(true);
        this.logStageScript(stage, stageScript);

        ProcessBuilder processBuilder = new ProcessBuilder(
                stageScript.getAbsolutePath(),
                this.context.workspace().getAbsolutePath(),
                this.context.sources().getAbsolutePath()
        ).directory(this.context.workspace());
        try {
            int status = this.createInvokerForStage(stage).exec(
                    processBuilder,
                    line -> lineLogger(logListener, line),
                    line -> lineLogger(logListener, line)
            );
            if(status == 0) {
                return StageTermination.Exit.SUCCESS;
            } else {
                return StageTermination.Exit.FAILURE;
            }
        } catch (InterruptedException e) {
            log.error("error processing stage script", e);
            return StageTermination.Exit.FAILURE;
        }
    }

    private void lineLogger(StageLogListener logListener, String line) {
        log.info(line);
        logListener.logLine(line);
    }

    private ProcessInvoker createInvokerForStage(StageHolder stage) {
        return new ProcessInvoker(Optional.ofNullable(stage.stage().timeout()).orElse(5L), TimeUnit.MINUTES);
    }

    private void logStageScript(StageHolder stage, File stageScript) {
        log.info("will execute stage {} / {} script from file {} with content : \n{}",
                stage.type().name().toLowerCase(), stage.stage().name(), stageScript, this.content(stageScript)
                );
    }

    private String content(File stageScript) {
        StringBuilder result = new StringBuilder();
        try(Reader reader = new FileReader(stageScript)) {
            char [] buffer = new char[1024];
            for(int read = reader.read(buffer) ; read != -1 ; read = reader.read(buffer)) {
                result.append(buffer, 0, read);
            }
        } catch (IOException e) {
            log.error("failed reading stage script", e);
        }

        return result.toString();
    }

    private File createStageScript(StageHolder stage) throws IOException {
        File stageScript = File.createTempFile(this.context.pipelineId() + "-" + stage.type().name().toLowerCase() + "-stage-" + stage.stage().name(), ".sh");
        stageScript.deleteOnExit();
        try(OutputStream out = new FileOutputStream(stageScript)) {
            this.pipelineScript.forStage(stage, out);
        }
        return stageScript;
    }

    private void ensureStageExists(StageHolder stage) throws IOException {
        stage.opt().stage().orElseThrow(() -> new IOException("malformed stage : " + stage));
    }

    private ValueList<Stage> stagesForType(StageHolder.Type type) {
        switch (type) {
            case MAIN:
                return this.context.pipeline().stages();
            case SUCCESS:
                return this.context.pipeline().onSuccess();
            default:
                return this.context.pipeline().onError();
        }
    }
}
