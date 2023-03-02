package org.codingmatters.poom.ci.apps;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.apps.releaser.Workspace;
import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.git.GithubRepositoryUrlProvider;
import org.codingmatters.poom.ci.apps.releaser.graph.GraphWalkResult;
import org.codingmatters.poom.ci.apps.releaser.graph.GraphWalker;
import org.codingmatters.poom.ci.apps.releaser.graph.PropagationContext;
import org.codingmatters.poom.ci.apps.releaser.graph.descriptors.RepositoryGraph;
import org.codingmatters.poom.ci.apps.releaser.graph.descriptors.RepositoryGraphDescriptor;
import org.codingmatters.poom.ci.apps.releaser.notify.Notifier;
import org.codingmatters.poom.ci.apps.releaser.task.*;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIRequesterClient;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Arguments;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class App {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(App.class);

    /**
     * RELEASE GRAPH
     * mvn exec:java -Dexec.mainClass=org.codingmatters.poom.ci.apps.App -Dexec.args="release-graph /tmp/playground/graph.yml"
     *
     * PROPAGATE VERSIONS
     * mvn exec:java -Dexec.mainClass=org.codingmatters.poom.ci.apps.App -Dexec.args="propagate-versions /tmp/playground/graph.yml"
     * @param args
     */
    public static void main(String[] args) {
        Arguments arguments = Arguments.from(args);

        if(arguments.argumentCount() >= 1 && arguments.arguments().get(0).equals("version")) {
            System.out.println(App.class.getPackage().getImplementationVersion());
            System.exit(0);
        }

        System.out.println("Releaser version : " + App.class.getPackage().getImplementationVersion());
        System.out.println("Called with : " + (args == null ? "" : Arrays.stream(args).collect(Collectors.joining(" "))));

        if(arguments.argumentCount() < 1) {
            usageAndFail(args);
        }

        if(arguments.arguments().get(0).equals("help")) {
            usage(System.out, args);
            System.exit(0);
        }

        CommandHelper commandHelper = new CommandHelper(line -> System.out.println(line), line -> System.err.println(line));

        JsonFactory jsonFactory = new JsonFactory();
        String pipelineUrl = Env.optional("PIPELINES_URL")
                .orElseGet(() -> new Env.Var("https://pipelines.ci.flexio.io/pipelines")).asString() ;

        HttpClientWrapper httpClientWrapper = OkHttpClientWrapper.build();
        PoomCIPipelineAPIClient client = new PoomCIPipelineAPIRequesterClient(
                new OkHttpRequesterFactory(httpClientWrapper, () -> pipelineUrl),
                jsonFactory,
                pipelineUrl
        );

        Notifier notifier = Notifier.fromArguments(httpClientWrapper, jsonFactory, commandHelper, arguments);

        GraphTaskListener graphTaskListener = new GraphTaskListener() {
            @Override
            public void info(GraphWalkResult result) {
                System.out.println(result.message());
            }

            @Override
            public void error(GraphWalkResult result) {
                System.err.println(result);
                System.exit(2);
            }
        };

        Workspace workspace = Workspace.temporary();
        try {
            if (arguments.arguments().get(0).equals("release")) {
                Arguments.OptionValue repository = arguments.option("repository");
                if (!repository.isPresent()) {
                    usageAndFail(args);
                }
                if (repository.get() == null) {
                    usageAndFail(args);
                }

                try {
                    ReleaseTaskResult result = new ReleaseTask(repository.get(), GithubRepositoryUrlProvider.ssh(), commandHelper, client, workspace).call();
                    if (result.exitStatus().equals(ReleaseTaskResult.ExitStatus.SUCCESS)) {
                        System.out.println(result.message());
                        System.exit(0);
                    } else {
                        System.err.println(result.message());
                        System.exit(2);
                    }
                } catch (Exception e) {
                    log.error("failed executing release", e);
                    System.exit(3);
                }
            } else if (arguments.arguments().get(0).equals("release-graph")) {
                if (arguments.argumentCount() < 1) {
                    usageAndFail(args);
                }
                try {
                    List<RepositoryGraphDescriptor> descriptorList = buildFilteredGraphDescriptorList(arguments);
                    System.out.println("Will release dependency graphs : " + descriptorList);

                    GraphTaskResult result = new ReleaseGraphTask(descriptorList, commandHelper, client, workspace, notifier, GithubRepositoryUrlProvider.ssh(), graphTaskListener).call();
                    System.out.println("\n\n\n\n####################################################################################");
                    System.out.println("####################################################################################");
                    System.out.printf("%s, released versions are :\n", result.message());
                    System.out.println(result.propagationContext().text());
                    System.out.println("####################################################################################");
                    System.out.println("####################################################################################\n\n");
                    System.exit(0);
                } catch (Exception e) {
                    log.error("failed executing release-graph", e);
                    notifier.notifyError("release-graph", "FAILURE", e);
                    System.exit(3);
                }
            } else if (arguments.arguments().get(0).equals("propagate-versions")) {
                if (arguments.argumentCount() < 1) {
                    usageAndFail(args);
                }
                try {
                    List<RepositoryGraphDescriptor> descriptorList = buildFilteredGraphDescriptorList(arguments);
                    System.out.println("Will propagate develop version for dependency graph : " + descriptorList);

                    GraphTaskResult result = new PropagateGraphVersionsTask(descriptorList, Optional.ofNullable(arguments.option("branch").get()), commandHelper, client, workspace, notifier, GithubRepositoryUrlProvider.ssh(), graphTaskListener).call();

                    System.out.println("\n\n\n\n####################################################################################");
                    System.out.println("####################################################################################");
                    System.out.printf("%s, propagated versions are :\n", result.message());
                    System.out.println(result.propagationContext().text());
                    System.out.println("####################################################################################");
                    System.out.println("####################################################################################\n\n");

                    System.exit(0);
                } catch (Exception e) {
                    log.error("failed executing release-graph", e);
                    notifier.notifyError("propagate-versions", "FAILURE", e);
                    System.exit(3);
                }
            } else {
                usageAndFail(args);
            }
        } finally {
            if(arguments.option("keep-workspace").isPresent()) {
                System.out.println("workspace kept in : " + workspace.path());
            } else {
                workspace.delete();
            }
        }
    }

    private static String formattedRepositoryList(List<RepositoryGraphDescriptor> descriptorList) {
        StringBuilder result = new StringBuilder();
        result.append("Repositories :");
        for (RepositoryGraphDescriptor descriptor : descriptorList) {
            appendRepos(result, descriptor.graph());
        }
        return result.toString();
    }

    private static void appendRepos(StringBuilder result, RepositoryGraph graph) {
        if(graph.opt().repositories().isPresent()) {
            for (String repository : graph.repositories()) {
                result.append("\n   - ").append(repository);
            }
        }
        if(graph.opt().then().isPresent()) {
            for (RepositoryGraph repositoryGraph : graph.then()) {
                appendRepos(result, repositoryGraph);
            }
        }
    }

    private static List<RepositoryGraphDescriptor> buildFilteredGraphDescriptorList(Arguments arguments) throws IOException {
        List<RepositoryGraphDescriptor> descriptorList = new LinkedList<>();
        for (int i = 1; i < arguments.argumentCount(); i++) {
            String graphFilePath = arguments.arguments().get(i);
            try (InputStream in = new FileInputStream(graphFilePath)) {
                descriptorList.add(RepositoryGraphDescriptor.fromYaml(in));
            }
        }

        if(arguments.option("from").isPresent()) {
            return RepositoryGraphDescriptor.filterFrom(arguments.option("from").get(), descriptorList);
        } else {
            return descriptorList;
        }
    }

    private static void usageAndFail(String[] args) {
        usage(System.err, args);
        System.exit(1);
    }

    private static void usage(PrintStream where, String[] args) {
        where.println("Usage :");
        where.println("   version");
        where.println("      prints the version");
        where.println("   help");
        where.println("      prints this usage message");
        where.println("   release --repository <repository, i.e. flexiooss/poom-ci>");
        where.println("      releases the repository and waits for the build pipeline to finish");
        where.println("   release-graph {--from <repo name>} <graph files>");
        where.println("      releases repository graphs");
        where.println("      --from   : using the from option, one can start releasing from one point in the graph");
        where.println("   propagate-versions {--from <repo name>} {--branch <branch name, defaults to develop>} <graph files>");
        where.println("      propagate versions in the repository graphs (version from preceding repos are propagated to following)");
        where.println("      --from   : using the from option, one can start propagating from one point in the graph");
        where.println("      --branch : by default, repos develop branch are used, one can change the branch using this option");
        where.println("");
        where.println("other options :");
        where.println("    --notify-url changes the default notify url");
        where.println("    --notify-bearer changes the default notify bearer");
    }
}
