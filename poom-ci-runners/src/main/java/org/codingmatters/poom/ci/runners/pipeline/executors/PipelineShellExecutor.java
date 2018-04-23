package org.codingmatters.poom.ci.runners.pipeline.executors;

import org.codingmatters.poom.ci.pipeline.PipelineScript;
import org.codingmatters.poom.ci.pipeline.api.types.StageTermination;
import org.codingmatters.poom.ci.pipeline.descriptors.Stage;
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
    public StageTermination.Exit execute(String stage, StageLogListener logListener) throws IOException {
        this.ensureStageExists(stage);

        File stageScript = this.createStageScript(stage);
        stageScript.setExecutable(true);
        this.logStageScript(stage, stageScript);

        ProcessBuilder processBuilder = new ProcessBuilder(
                stageScript.getAbsolutePath(),
                this.context.workspace().getParentFile().getAbsolutePath(),
                this.context.workspace().getAbsolutePath()
        ).directory(this.context.workspace());
        try {
            int status = this.creatInvokerForStage(stage).exec(
                    processBuilder,
                    line -> logListener.logLine(line),
                    line -> logListener.logLine(line)
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

    private ProcessInvoker creatInvokerForStage(String stageName) {
        Stage stage = this.context.pipeline().stages().stream().filter(st -> st.name().equals(stageName)).findFirst().get();
        return new ProcessInvoker(Optional.ofNullable(stage.timeout()).orElse(5L), TimeUnit.MINUTES);
    }

    private void logStageScript(String stage, File stageScript) {
        log.info("will execute stage {} script from file {} with content : \n{}",
                stage, stageScript, this.content(stageScript)
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

    private void stageError(String line) {
        log.error(line);
    }

    private void stageOutput(String line) {
        log.info(line);
    }

    private File createStageScript(String stage) throws IOException {
        File stageScript = File.createTempFile(this.context.pipelineId() + "-stage-" + stage, ".sh");
        stageScript.deleteOnExit();
        try(OutputStream out = new FileOutputStream(stageScript)) {
            this.pipelineScript.forStage(stage, out);
        }
        return stageScript;
    }

    private void ensureStageExists(String stage) throws IOException {
        this.context.pipeline().stages().stream()
                .filter(stg -> stg.name().equals(stage)).findFirst()
                .orElseThrow(() -> new IOException("no stage " + stage));
    }
}
