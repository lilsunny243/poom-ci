package org.codingmatters.poom.ci.runners.pipeline.providers.gh;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.pipeline.descriptors.json.PipelineReader;
import org.codingmatters.poom.ci.runners.pipeline.PipelineContext;
import org.codingmatters.poom.ci.runners.pipeline.PipelineVariables;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.process.ProcessInvoker;

import java.io.File;
import java.io.IOException;

public abstract class AbstractGitHubPipelineContextProvider implements PipelineContext.PipelineContextProvider {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(AbstractGitHubPipelineContextProvider.class);

    static public final String PROVIDER_WORKDIR_PROP = "gh.pipeline.provider.workdir";

    private final PoomCIPipelineAPIClient pipelineAPIClient;
    private final YAMLFactory yamlFactory;

    public AbstractGitHubPipelineContextProvider(PoomCIPipelineAPIClient pipelineAPIClient, YAMLFactory yamlFactory) {
        this.pipelineAPIClient = pipelineAPIClient;
        this.yamlFactory = yamlFactory;
    }

    protected abstract PipelineVariables createVariables(String pipelineId, PipelineTrigger trigger) throws ProcessingException;


    @Override
    public PipelineContext pipelineContext(String pipelineId, PipelineTrigger trigger) throws IOException {
        try {

            File workspace = this.createWorkspace(pipelineId);
            File sources = this.createSources(pipelineId);

            PipelineVariables vars = this.createVariables(pipelineId, trigger);

            this.checkoutTo(vars, sources);

            Pipeline pipeline = this.readPipeline(sources);

            return new PipelineContext(
                    vars,
                    pipeline,
                    workspace,
                    sources);
        } catch (ProcessingException e) {
            throw new IOException("failed creating pipeline context", e);
        }
    }

    private void checkoutTo(PipelineVariables vars, File workspace) throws ProcessingException {
        ProcessInvoker invoker = new ProcessInvoker();
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(workspace)
                ;

        int status;
        try {
            log.info("initializing local repo");
            status = invoker.exec(processBuilder.command("git", "init"), line -> log.info(line), line -> log.error(line));
            if(status != 0) throw new ProcessingException("git init exited with a none 0 status");

            log.info("fetching remote repo : {}, {}", vars.repositoryUrl(), vars.branch());
            status = invoker.exec(processBuilder.command("git", "fetch", "-u", vars.repositoryUrl(), vars.branch()), line -> log.info(line), line -> log.error(line));
            if(status != 0) throw new ProcessingException("git fetch exited with a none 0 status");

            String checkoutTarget = "FETCH_HEAD";
            if(vars.changeset() != null && ! vars.changeset().isEmpty()) {
                checkoutTarget = vars.changeset();
            }
            log.info("checking out {}", checkoutTarget);
            status = invoker.exec(processBuilder.command("git", "checkout", vars.changeset()), line -> log.info(line), line -> log.error(line));
            if (status != 0) throw new ProcessingException("git checkout exited with a none 0 status");
        } catch (InterruptedException | IOException e) {
            throw new ProcessingException("exception raised whlie checking out workspace", e);
        }
    }

    public PoomCIPipelineAPIClient pipelineAPIClient() {
        return pipelineAPIClient;
    }

    protected Pipeline readPipeline(File workspace) throws IOException {
        File pipelineDescriptor = new File(workspace, "poom-ci-pipeline.yaml");
        try(JsonParser parser = this.yamlFactory.createParser(pipelineDescriptor)) {
            return new PipelineReader().read(parser);
        }
    }


    protected File createWorkspace(String pipelineId) throws ProcessingException {
        return this.createPipelineDir(pipelineId, "workspace");
    }

    protected File createSources(String pipelineId) throws ProcessingException {
        return this.createPipelineDir(pipelineId, "sources");
    }

    private File createPipelineDir(String pipelineId, String type) throws ProcessingException {
        File workdir = new File(System.getProperty(PROVIDER_WORKDIR_PROP, System.getProperty("java.io.tmpdir")));
        workdir.mkdirs();

        File result;
        int suffix = 0;
        do {
            result = new File(workdir, String.format("%s-%s-%03d", pipelineId, type, suffix));
            suffix++;
        } while (result.exists());

        result.mkdirs();

        return result;
    }


    static public class ProcessingException extends Exception {
        public ProcessingException(String s) {
            super(s);
        }

        public ProcessingException(String s, Throwable throwable) {
            super(s, throwable);
        }
    }
}
