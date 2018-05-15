package org.codingmatters.poom.ci.runners.pipeline.providers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.pipeline.api.GithubTriggerGetResponse;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.pipeline.descriptors.json.PipelineReader;
import org.codingmatters.poom.ci.runners.pipeline.PipelineContext;
import org.codingmatters.poom.ci.runners.pipeline.providers.gh.Ref;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.poom.services.support.process.ProcessInvoker;

import java.io.File;
import java.io.IOException;

public class GithubPipelineContextProvider implements PipelineContext.PipelineContextProvider {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(GithubPipelineContextProvider.class);

    static public final String PROVIDER_WORKDIR_PROP = "gh.pipeline.provider.workdir";

    private final PoomCIPipelineAPIClient pipelineAPIClient;
    private final YAMLFactory yamlFactory;

    public GithubPipelineContextProvider(PoomCIPipelineAPIClient pipelineAPIClient, YAMLFactory yamlFactory) {
        this.pipelineAPIClient = pipelineAPIClient;
        this.yamlFactory = yamlFactory;
    }

    @Override
    public PipelineContext pipelineContext(String pipelineId, PipelineTrigger trigger) throws IOException {
        try {
            GithubPushEvent event = this.retrieveEvent(trigger);

            File workspace = this.createWorkspace(pipelineId);
            File sources = this.createSources(pipelineId);
            this.checkoutTo(event, sources);

            Pipeline pipeline = this.readPipeline(sources);

            return new PipelineContext(
                    pipelineId,
                    pipeline,
                    workspace,
                    sources,
                    event.repository().full_name(),
                    this.branchFromRef(event),
                    event.after());
        } catch (ProcessingException e) {
            throw new IOException("failed creating pipeline context", e);
        }
    }

    private String branchFromRef(GithubPushEvent event) {
//        String[] splitted = event.ref().split("\\/");
//        return splitted[splitted.length - 1];

        return new Ref(event.ref()).branch();
    }

    private Pipeline readPipeline(File workspace) throws IOException {
        File pipelineDescriptor = new File(workspace, "poom-ci-pipeline.yaml");
        try(JsonParser parser = this.yamlFactory.createParser(pipelineDescriptor)) {
            return new PipelineReader().read(parser);
        }
    }

    private void checkoutTo(GithubPushEvent event, File workspace) throws ProcessingException {
        ProcessInvoker invoker = new ProcessInvoker();
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(workspace)
                ;

        int status = 0;
        try {
            status = invoker.exec(processBuilder.command("git", "init"), line -> log.info(line), line -> log.error(line));
            if(status != 0) throw new ProcessingException("git init exited with a none 0 status");

            String url = event.repository().clone_url();
            if(Env.optional("GH_PIPE_USE_SSH").equals("true")) {
                url = event.repository().ssh_url();
            }

            status = invoker.exec(processBuilder.command("git", "fetch", "-u", url, branchFromRef(event)), line -> log.info(line), line -> log.error(line));
            if(status != 0) throw new ProcessingException("git fetch exited with a none 0 status");

            status = invoker.exec(processBuilder.command("git", "checkout", event.after()), line -> log.info(line), line -> log.error(line));
            if(status != 0) throw new ProcessingException("git checkout exited with a none 0 status");
        } catch (InterruptedException | IOException e) {
            throw new ProcessingException("exception raised whlie checking out workspace", e);
        }
    }

    private File createWorkspace(String pipelineId) throws ProcessingException {
        return this.createPipelineDir(pipelineId, "workspace");
    }

    private File createSources(String pipelineId) throws ProcessingException {
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

    private GithubPushEvent retrieveEvent(PipelineTrigger trigger) throws ProcessingException {
        try {
            GithubTriggerGetResponse response = this.pipelineAPIClient.triggers().githubTriggers().githubTrigger().get(req -> req.triggerId(trigger.triggerId()));

            return response.opt().status200().payload()
                    .orElseThrow(() -> {
                        String token = log.tokenized().error("while retrieving github push event, received unexpected response : {}", response);
                        return new ProcessingException("error getting pipeline trigger, see logs with token " + token);
                    });
        } catch (IOException e) {
            throw new ProcessingException("failed accessing pipeline api");
        }
    }

    class ProcessingException extends Exception {
        public ProcessingException(String s) {
            super(s);
        }

        public ProcessingException(String s, Throwable throwable) {
            super(s, throwable);
        }
    }
}
