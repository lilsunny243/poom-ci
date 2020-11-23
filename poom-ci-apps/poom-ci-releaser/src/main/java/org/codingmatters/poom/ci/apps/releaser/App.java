package org.codingmatters.poom.ci.apps.releaser;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.command.exception.CommandFailed;
import org.codingmatters.poom.ci.apps.releaser.git.Git;
import org.codingmatters.poom.ci.apps.releaser.graph.GraphWalkResult;
import org.codingmatters.poom.ci.apps.releaser.graph.GraphWalker;
import org.codingmatters.poom.ci.apps.releaser.graph.PropagationContext;
import org.codingmatters.poom.ci.apps.releaser.graph.descriptors.RepositoryGraph;
import org.codingmatters.poom.ci.apps.releaser.graph.descriptors.RepositoryGraphDescriptor;
import org.codingmatters.poom.ci.apps.releaser.task.PropagateVersionsTask;
import org.codingmatters.poom.ci.apps.releaser.task.ReleaseTask;
import org.codingmatters.poom.ci.apps.releaser.task.ReleaseTaskResult;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIRequesterClient;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Arguments;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class App {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(App.class);

    /**
     * RELEASE GRAPH
     * mvn exec:java -Dexec.mainClass=org.codingmatters.poom.ci.apps.releaser.App -Dexec.args="release-graph /tmp/playground/graph.yml"
     *
     * PROPAGATE VERSIONS
     * mvn exec:java -Dexec.mainClass=org.codingmatters.poom.ci.apps.releaser.App -Dexec.args="propagate-versions /tmp/playground/graph.yml"
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
                    ReleaseTaskResult result = new ReleaseTask(repository.get(), commandHelper, client, workspace).call();
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
                    notify(arguments.arguments().get(0), "START", formattedRepositoryList(descriptorList), httpClientWrapper, jsonFactory, commandHelper, arguments);

                    ExecutorService pool = Executors.newFixedThreadPool(10);
                    GraphWalker.WalkerTaskProvider walkerTaskProvider = (repository, context) -> new ReleaseTask(repository, context, commandHelper, client, workspace);

                    PropagationContext propagationContext = new PropagationContext();
                    for (RepositoryGraphDescriptor descriptor : descriptorList) {
                        walkGraph(descriptor, propagationContext, pool, walkerTaskProvider);
                    }

                    System.out.println("\n\n\n\n####################################################################################");
                    System.out.println("####################################################################################");
                    System.out.printf("Finished releasing graphs, released versions are :\n");
                    System.out.println(propagationContext.text());
                    System.out.println("####################################################################################");
                    System.out.println("####################################################################################\n\n");

                    notify(arguments.arguments().get(0), "DONE", propagationContext.text(), httpClientWrapper, jsonFactory, commandHelper, arguments);
                    System.exit(0);
                } catch (Exception e) {
                    log.error("failed executing release-graph", e);
                    notifyError(arguments.arguments().get(0), "FAILURE", e, arguments, commandHelper, jsonFactory, httpClientWrapper);
                    System.exit(3);
                }
            } else if (arguments.arguments().get(0).equals("propagate-versions")) {
                if (arguments.argumentCount() < 1) {
                    usageAndFail(args);
                }
                try {
                    List<RepositoryGraphDescriptor> descriptorList = buildFilteredGraphDescriptorList(arguments);
                    System.out.println("Will propagate develop version for dependency graph : " + descriptorList);
                    notify(arguments.arguments().get(0), "START", formattedRepositoryList(descriptorList), httpClientWrapper, jsonFactory, commandHelper, arguments);
                    ExecutorService pool = Executors.newFixedThreadPool(10);

                    GraphWalker.WalkerTaskProvider walkerTaskProvider = (repository, context) -> {
                        String branch = "develop";
                        if (arguments.option("branch").isPresent()) {
                            branch = arguments.option("branch").get();
                        }
                        return new PropagateVersionsTask(repository, branch, context, commandHelper, client, workspace);
                    };

                    PropagationContext propagationContext = new PropagationContext();
                    for (RepositoryGraphDescriptor descriptor : descriptorList) {
                        walkGraph(descriptor, propagationContext, pool, walkerTaskProvider);
                    }

                    System.out.println("\n\n\n\n####################################################################################");
                    System.out.println("####################################################################################");
                    System.out.printf("Finished propagating versions, propagated versions are :\n");
                    System.out.println(propagationContext.text());
                    System.out.println("####################################################################################");
                    System.out.println("####################################################################################\n\n");

                    notify(arguments.arguments().get(0), "DONE", propagationContext.text(), httpClientWrapper, jsonFactory, commandHelper, arguments);
                    System.exit(0);
                } catch (Exception e) {
                    log.error("failed executing release-graph", e);
                    notifyError(arguments.arguments().get(0), "FAILURE", e, arguments, commandHelper, jsonFactory, httpClientWrapper);
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

    private static void notifyError(String action, String stage, Exception e, Arguments arguments, CommandHelper commandHelper, JsonFactory jsonFactory, HttpClientWrapper httpClientWrapper) {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream() ; PrintStream stream = new PrintStream(out)) {
            e.printStackTrace(stream);
            stream.flush();
            stream.close();
            notify(action, stage, out.toString(), httpClientWrapper, jsonFactory, commandHelper, arguments);
        } catch (IOException ioException) {
            log.warn("error notifying error...", ioException);
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

    @NotNull
    private static List<RepositoryGraphDescriptor> buildFilteredGraphDescriptorList(Arguments arguments) throws IOException {
        List<RepositoryGraphDescriptor> descriptorList = new LinkedList<>();
        boolean searchStartFrom = arguments.option("from").isPresent();
        for (int i = 1; i < arguments.argumentCount(); i++) {
            String graphFilePath = arguments.arguments().get(i);
            RepositoryGraphDescriptor descriptor;
            try (InputStream in = new FileInputStream(graphFilePath)) {
                descriptor = RepositoryGraphDescriptor.fromYaml(in);
                if(searchStartFrom) {
                    if (descriptor.containsRepository(arguments.option("from").get())) {
                        descriptor = descriptor.subgraph(arguments.option("from").get());
                        descriptorList.add(descriptor);
                        searchStartFrom = false;
                    } else {
                        continue;
                    }
                } else {
                    descriptorList.add(descriptor);
                }
            }
        }
        return descriptorList;
    }

    private static void walkGraph(RepositoryGraphDescriptor descriptor, PropagationContext propagationContext, ExecutorService pool, GraphWalker.WalkerTaskProvider walkerTaskProvider) throws InterruptedException, java.util.concurrent.ExecutionException {
        GraphWalker releaseWalker = new GraphWalker(
                descriptor,
                propagationContext,
                walkerTaskProvider,
                pool
        );
        GraphWalkResult result = pool.submit(releaseWalker).get();
        if(result.exitStatus().equals(ReleaseTaskResult.ExitStatus.SUCCESS)) {
            System.out.println(result.message());
        } else {
            System.err.println(result);
            System.exit(2);
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

    private static void notify(String action, String stage, String message, HttpClientWrapper httpClientWrapper, JsonFactory jsonFactory, CommandHelper commandHelper, Arguments arguments) throws IOException {
        String url = "https://api.flexio.io/httpin/my/in/5fb7c9b2a6a8c401ab4f4665";
        String bearer = "fd62b406-9ccd-4bb5-89a9-3868c395a15e";
        if(arguments.option("notify-bearer").isPresent()) {
            bearer = arguments.option("notify-bearer").get();
        }
        if(arguments.option("notify-url").isPresent()) {
            url = arguments.option("notify-url").get();
        }

        System.out.println("notifying...");

        Git git = new Git(new File("/tmp"), commandHelper);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        payload.put("stage", stage);
        payload.put("message", message);
        try {
            payload.put("username", git.username());
            payload.put("email", git.email());
        } catch (CommandFailed e) {
            log.warn("failed getting username / email", e);
        }
        try(Response response = httpClientWrapper.execute(new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + bearer)
                .post(RequestBody.create(new ObjectMapper(jsonFactory).writeValueAsBytes(payload)))
                .build())) {
            if(response.code() != 200 && response.code() != 204) {
                System.err.println("whlie notifying got status code " + response.code());
                System.err.println("response was : " + response);
            }
        }
    }
}
