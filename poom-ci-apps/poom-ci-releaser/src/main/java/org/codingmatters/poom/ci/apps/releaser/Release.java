package org.codingmatters.poom.ci.apps.releaser;

import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.command.exception.CommandFailed;
import org.codingmatters.poom.ci.apps.releaser.flow.FlexioFlow;
import org.codingmatters.poom.ci.apps.releaser.git.Git;
import org.codingmatters.poom.ci.apps.releaser.git.GitRepository;
import org.codingmatters.poom.ci.apps.releaser.graph.PropagationContext;
import org.codingmatters.poom.ci.apps.releaser.maven.Pom;
import org.codingmatters.poom.ci.apps.releaser.maven.pom.ArtifactCoordinates;

import java.io.*;
import java.util.UUID;

public class Release {
    private final String repositoryUrl;
    private final PropagationContext propagationContext;
    private final CommandHelper commandHelper;

    public Release(String repositoryUrl, PropagationContext propagationContext, CommandHelper commandHelper) {
        this.repositoryUrl = repositoryUrl;
        this.propagationContext = propagationContext;
        this.commandHelper = commandHelper;
    }

    public ArtifactCoordinates initiate() throws CommandFailed {
        File workspace = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID().toString());
        workspace.mkdir();

        GitRepository repository = new Git(workspace, this.commandHelper).clone(this.repositoryUrl);
        FlexioFlow flow = new FlexioFlow(workspace, this.commandHelper);
        repository.checkout("master");
        repository.checkout("develop");

        ArtifactCoordinates coordinates = this.readPom(workspace).project();

        System.out.println("\n\n\n\n####################################################################################");
        System.out.printf("Starting release of %s with context :\n", coordinates.coodinates());
        System.out.println(this.propagationContext.text());
        System.out.println("####################################################################################\n\n");

        flow.startRelease();
        String releaseVersion = flow.version();
        repository.merge("master", "release auto merge");
        if(! this.propagationContext.iEmpty()) {
            try {
                Pom currentPom = this.readPom(workspace);
                Pom upgradedPom = this.propagationContext.applyTo(currentPom);
                if(upgradedPom.changedFrom(currentPom)) {
                    System.out.println("\n\n####################################################################################");
                    System.out.println("Versions propagated, need to write and commit");
                    System.out.println("####################################################################################\n\n");
                    this.writePom(workspace, upgradedPom);
                    repository.commit("propagating versions : \n" + this.propagationContext.text());
                }
            } catch (IOException e) {
                throw new CommandFailed("failed propagating versions", e);
            }
        }

        System.out.println("\n\n####################################################################################");
        System.out.println("Ready for release...");
        System.out.println("####################################################################################\n\n");

        flow.finishRelease();

        ArtifactCoordinates result = new ArtifactCoordinates(coordinates.getGroupId(), coordinates.getArtifactId(), releaseVersion);

        System.out.println("\n\n####################################################################################");
        System.out.printf("%s released.\n", result.coodinates());
        System.out.println("####################################################################################\n\n\n\n");

        return result;
    }

    private Pom readPom(File workspace) throws CommandFailed {
        try(InputStream pomFile = new FileInputStream(new File(workspace, "pom.xml"))) {
            return Pom.from(pomFile);
        } catch (IOException e) {
            throw new CommandFailed("failed reading pom for " + this.repositoryUrl, e);
        }
    }

    private void writePom(File workspace, Pom pom) throws CommandFailed {
        try(Writer writer = new FileWriter(new File(workspace, "pom.xml"))) {
            pom.writeTo(writer);
        } catch (IOException e) {
            throw new CommandFailed("failed writing pom", e);
        }
    }
}
