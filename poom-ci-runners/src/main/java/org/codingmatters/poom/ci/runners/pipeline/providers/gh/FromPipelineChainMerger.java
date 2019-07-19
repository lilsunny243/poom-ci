package org.codingmatters.poom.ci.runners.pipeline.providers.gh;

import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.pipeline.descriptors.RepositorySpec;
import org.codingmatters.poom.ci.pipeline.descriptors.ValueList;
import org.codingmatters.poom.ci.pipeline.merge.SimplePipelineMerger;
import org.codingmatters.poom.ci.runners.git.CloneRepository;
import org.codingmatters.poom.ci.runners.pipeline.NotAPipelineContextException;
import org.codingmatters.poom.ci.runners.pipeline.PipelineDescriptoReader;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class FromPipelineChainMerger implements Closeable {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(FromPipelineChainMerger.class);

    static private final Function<RepositorySpec, CloneRepository> CREATE_DEFAULT_CLONER = spec -> new CloneRepository(spec.url(), spec.branch(), null);

    private final PipelineDescriptoReader pipelineDescriptoReader;
    private Pipeline pipeline;
    private Function<RepositorySpec, CloneRepository> clonerSupplier;
    private List<File> checkoutDirs = new LinkedList<>();

    public FromPipelineChainMerger(Pipeline pipeline, PipelineDescriptoReader pipelineDescriptoReader) throws AbstractGitHubPipelineContextProvider.ProcessingException, NotAPipelineContextException, IOException {
        this(pipeline, pipelineDescriptoReader, CREATE_DEFAULT_CLONER);
    }

    protected FromPipelineChainMerger(Pipeline pipeline, PipelineDescriptoReader pipelineDescriptoReader, Function<RepositorySpec, CloneRepository> clonerSupplier) throws AbstractGitHubPipelineContextProvider.ProcessingException, NotAPipelineContextException, IOException {
        this.pipelineDescriptoReader = pipelineDescriptoReader;
        this.pipeline = pipeline;
        this.clonerSupplier = clonerSupplier;

        this.buildFromDirs(pipeline.from());
        log.debug("checkout dirs : {}", this.checkoutDirs);
    }

    private void buildFromDirs(ValueList<RepositorySpec> from) throws AbstractGitHubPipelineContextProvider.ProcessingException, NotAPipelineContextException, IOException {
        if(from == null) return;

        for (RepositorySpec spec : from) {
            File checkout = this.checkout(spec);

            Pipeline fromPipeline = this.pipelineDescriptoReader.read(checkout);
            this.buildFromDirs(fromPipeline.from());

            this.checkoutDirs.add(checkout);
        }
    }

    private File checkout(RepositorySpec spec) throws AbstractGitHubPipelineContextProvider.ProcessingException {
        File workdir = this.createTemporaryWorkdir();
        this.createCloner(spec).to(workdir);
        return workdir;
    }

    private CloneRepository createCloner(RepositorySpec spec) {
        return this.clonerSupplier.apply(spec);
    }

    private File createTemporaryWorkdir() {
        File result = new File(
                Env.optional("PIPELINE_CHAIN_TEMPORARY_DIR").orElse(new Env.Var(System.getProperty("java.io.tmpdir"))).asFile(),
                UUID.randomUUID().toString()
        );
        result.mkdirs();
        return result;
    }

    public void mergeSources(File sources) throws IOException {
        for (File checkoutDir : this.checkoutDirs) {
            this.mergeDir(checkoutDir, sources, "");
        }

    }

    private void mergeDir(File dir, File into, String path) throws IOException {
        for (File file : dir.listFiles()) {
            if(file.isDirectory()) {
                File target = new File(into, file.getName());
                target.mkdirs();
                this.mergeDir(file, target, path + "/" + file.getName());
            } else if(! this.excluded(path + "/" + file.getName())){
                this.copy(file, into);
            }
        }
    }

    private boolean excluded(String path) {
        return path.equals("/poom-ci-pipeline.yaml")
                || path.equals("/LICENSE")
                || path.equals("/README.md")
                || path.equals("/readme.md")
                || path.equals("/flexio-flow.yml")
                || path.equals("/.gitignore")
                ;
    }

    private void copy(File file, File dir) throws IOException {
        try(InputStream in = new FileInputStream(file); OutputStream out = new FileOutputStream(new File(dir, file.getName()))) {
            byte[] buffer = new byte[1024];
            for(int read = in.read(buffer) ; read != -1 ; read = in.read(buffer)) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }

    public Pipeline mergedPipeline() throws NotAPipelineContextException, IOException {
        SimplePipelineMerger merger = new SimplePipelineMerger();
        Pipeline result = this.pipeline;
        for (File checkoutDir : this.checkoutDirs) {
            Pipeline p = this.pipelineDescriptoReader.read(checkoutDir);
            result = merger.merge(p, result);
        }
        return result;
    }

    @Override
    public void close() {
        for (File dir : this.checkoutDirs) {
            try {
                this.delete(dir);
            } catch (Exception e) {
                log.warn("failed to delete checkout dir : " + dir.getAbsolutePath(), e);
            }
        }
    }

    private void delete(File file) {
        if(file.isDirectory()) {
            for (File sub : file.listFiles()) {
                this.delete(sub);
            }
        }
        file.delete();
    }
}
