package org.codingmatters.poom.ci.apps.releaser.task;

import org.codingmatters.poom.ci.apps.releaser.ProjectDescriptor;
import org.codingmatters.poom.ci.apps.releaser.RepositoryPipeline;
import org.codingmatters.poom.ci.apps.releaser.Workspace;
import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.command.exception.CommandFailed;
import org.codingmatters.poom.ci.apps.releaser.flow.FlexioFlow;
import org.codingmatters.poom.ci.apps.releaser.git.Git;
import org.codingmatters.poom.ci.apps.releaser.git.GitRepository;
import org.codingmatters.poom.ci.apps.releaser.graph.PropagationContext;
import org.codingmatters.poom.ci.apps.releaser.hb.JsPackage;
import org.codingmatters.poom.ci.apps.releaser.maven.Pom;
import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.pipeline.api.types.pipeline.Status;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.date.UTC;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

public class PropagateVersionsTask implements Callable<ReleaseTaskResult> {
    static private CategorizedLogger log = CategorizedLogger.getLogger(PropagateVersionsTask.class);

    private final String repository;
    private final String repositoryUrl;
    private final String branch;
    private final PropagationContext propagationContext;
    private final CommandHelper commandHelper;
    private final PoomCIPipelineAPIClient client;
    private final Workspace workspace;

    public PropagateVersionsTask(String repository, String branch, CommandHelper commandHelper, PoomCIPipelineAPIClient client, Workspace workspace) {
        this(repository, branch, new PropagationContext(), commandHelper, client, workspace);
    }

    public PropagateVersionsTask(String repository, String branch, PropagationContext propagationContext, CommandHelper commandHelper, PoomCIPipelineAPIClient client, Workspace workspace) {
        this.repository = repository;
        this.repositoryUrl = String.format("git@github.com:%s.git", repository);
        this.branch = branch;
        this.propagationContext = propagationContext;
        this.commandHelper = commandHelper;
        this.client = client;
        this.workspace = workspace;
    }

    @Override
    public ReleaseTaskResult call() throws Exception {
        LocalDateTime start = UTC.now();
        try {
            System.out.println("####################################################################################");
            System.out.printf("Propagating versions for %s/%s with context :\n", this.repository, this.branch);
            System.out.println(this.propagationContext.text());
            System.out.println("####################################################################################");
//        1. checkout this.branch
            File repoDir = this.workspace.mkdir(UUID.randomUUID().toString());
            repoDir.mkdir();

            GitRepository repository = new Git(repoDir, this.commandHelper).clone(this.repositoryUrl);
            FlexioFlow flow = new FlexioFlow(repoDir, this.commandHelper);
            repository.checkout(this.branch);

//        1. read pom
            ProjectDescriptor currentPom = this.readProjectDescriptor(repoDir);
//        2. upgrade parent, deps and plugins
            ProjectDescriptor upgradedPom = this.propagationContext.applyTo(currentPom);
//            2.1. if propagationContext has updates
            if (upgradedPom.changedFrom(currentPom)) {
//            2.2. commit and wait for build
                this.writeProjectDescriptor(repoDir, upgradedPom);
                flow.commit("propagating versions : \n" + this.propagationContext.text());
                this.waitForBuild(start);
            }
//        3. add coordinates to propagationContext
            this.propagationContext.addPropagatedArtifact(currentPom.project());

            return new ReleaseTaskResult(ReleaseTaskResult.ExitStatus.SUCCESS, "versions propagated to " + this.repository + "/" + this.branch, currentPom.project());
        } catch (CommandFailed e) {

            return new ReleaseTaskResult(ReleaseTaskResult.ExitStatus.FAILURE, "failure propagating versions to " + this.repository + "/" + this.branch, null);
        }

    }

    private void waitForBuild(LocalDateTime start) throws IOException, InterruptedException {
        RepositoryPipeline pipeline = new RepositoryPipeline(this.repository, this.branch, this.client);
        Thread.sleep(2000L);
        Optional<Pipeline> pipe = pipeline.last(start);
        if (!pipe.isPresent()) {
            System.out.println("Waiting for build pipeline to start...");
            do {
                Thread.sleep(2000L);
                pipe = pipeline.last(start);
            } while (!pipe.isPresent());
        }

        System.out.printf("waiting for pipeline %s to finish...\n", pipe.get().opt().id().orElse("NONE"));
        while (!pipe.get().opt().status().run().orElse(Status.Run.PENDING).equals(Status.Run.DONE)) {
            Thread.sleep(2000L);
            pipe = pipeline.last(start);
        }
    }

    private ProjectDescriptor readProjectDescriptor(File workspace) throws CommandFailed {
        if(new File(workspace, "pom.xml").exists()) {
            try (InputStream pjDescFile = new FileInputStream(new File(workspace, "pom.xml"))) {
                return Pom.from(pjDescFile);
            } catch (IOException e) {
                throw new CommandFailed("failed reading pom for " + this.repositoryUrl, e);
            }
        } else
        if(new File(workspace, "package.json").exists()) {
            try (InputStream pjDescFile = new FileInputStream(new File(workspace, "package.json"))) {
                return JsPackage.read(pjDescFile);
            } catch (IOException e) {
                throw new CommandFailed("failed reading pom for " + this.repositoryUrl, e);
            }
        } else {
            return null;
        }
    }

    private void writeProjectDescriptor(File workspace, ProjectDescriptor projectDescriptor) throws CommandFailed {
        try(Writer writer = new FileWriter(new File(workspace, projectDescriptor.defaultFilename()))) {
            projectDescriptor.writeTo(writer);
        } catch (IOException e) {
            throw new CommandFailed("failed writing pom", e);
        }
    }
}
