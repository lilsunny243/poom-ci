package org.codingmatters.poom.ci.runners.pipeline.executors;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.ci.ciphering.DataUncipherer;
import org.codingmatters.poom.ci.ciphering.descriptors.CipheredData;
import org.codingmatters.poom.ci.ciphering.descriptors.json.CipheredDataReader;
import org.codingmatters.poom.ci.pipeline.PipelineScript;
import org.codingmatters.poom.ci.pipeline.api.types.StageTermination;
import org.codingmatters.poom.ci.pipeline.descriptors.Secret;
import org.codingmatters.poom.ci.pipeline.descriptors.StageHolder;
import org.codingmatters.poom.ci.pipeline.stage.OnlyWhenParsingException;
import org.codingmatters.poom.ci.pipeline.stage.OnlyWhenProcessor;
import org.codingmatters.poom.ci.runners.pipeline.PipelineContext;
import org.codingmatters.poom.ci.runners.pipeline.PipelineExecutor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.process.ProcessInvoker;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class PipelineShellExecutor implements PipelineExecutor {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PipelineShellExecutor.class);

    private final PipelineContext context;
    private final PipelineScript pipelineScript;
    private final KeyStore keystore;
    private final char[] keypass;
    private final JsonFactory jsonFactory;
    private final Map<String, String> secretVars = new HashMap<>();

    public PipelineShellExecutor(PipelineContext context, KeyStore keystore, char[] keypass, JsonFactory jsonFactory) {
        this.context = context;
        this.keystore = keystore;
        this.keypass = keypass;
        this.jsonFactory = jsonFactory;
        this.pipelineScript = new PipelineScript(this.context.pipeline());
    }

    @Override
    public void initialize() throws IOException {
        if(this.context.pipeline().secrets() != null) {
            for (Secret secret : this.context.pipeline().secrets()) {
                byte[] data = this.readSecretData(secret);
                if(secret.as().equals(Secret.As.file)) {
                    // pipeline.secrets of type file unciphered to $WORKSPACE/secrets/{secret.name}
                    this.writeSecretToFile(secret, data);
                } else {
                    this.secretVars.put(secret.name(), new String(data));
                }
            }
        }

    }

    @Override
    public boolean isExecutable(StageHolder stage) throws InvalidStageRestrictionException {
        try {
            return new OnlyWhenProcessor(this.context.variableProvider()).isExecutable(stage.stage());
        } catch (OnlyWhenParsingException e) {
            throw new InvalidStageRestrictionException(
                    String.format("error evaluating stage %s (%s) onlyWhen expressions", stage.stage().name(), stage.type()),
                    e);
        }
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

        this.context.setVariablesTo(processBuilder.environment());

        if(! this.secretVars.isEmpty()) {
            processBuilder.environment().putAll(this.secretVars);
        }

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

    private byte[] readSecretData(Secret secret) throws IOException {
        try {
            return new DataUncipherer(this.keystore, this.keypass).uncipher(this.readCipheredDataFile(secret));
        } catch (CertificateException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | InvalidKeyException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            throw new IOException("error reading / unciphering secret data", e);
        }
    }

    private CipheredData readCipheredDataFile(Secret secret) throws IOException {
        String secretPath = secret.content().replaceAll("\\$SRC", this.context.sources().getAbsolutePath())
                .replaceAll("\\$\\{SRC\\}", this.context.sources().getAbsolutePath());

        try(JsonParser jsonParser = this.jsonFactory.createParser(new File(secretPath))) {
            return new CipheredDataReader().read(jsonParser);
        } catch (IOException e) {
            throw new IOException("failed reading secret file : " + secretPath, e);
        }
    }

    private void writeSecretToFile(Secret secret, byte[] data) throws IOException {
        File secretFile = new File(new File(this.context.workspace(), "secrets"), secret.name());
        secretFile.getParentFile().mkdirs();
        try(OutputStream out = new FileOutputStream(secretFile)) {
            out.write(data);
            out.flush();
        } catch (IOException e) {
            throw new IOException("failed writing secret file to " + secretFile.getAbsolutePath(), e);
        }
    }
}
