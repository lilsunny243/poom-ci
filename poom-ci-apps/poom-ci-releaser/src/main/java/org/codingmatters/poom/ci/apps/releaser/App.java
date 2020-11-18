package org.codingmatters.poom.ci.apps.releaser;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.graph.GraphWalkResult;
import org.codingmatters.poom.ci.apps.releaser.graph.GraphWalker;
import org.codingmatters.poom.ci.apps.releaser.graph.descriptors.RepositoryGraphDescriptor;
import org.codingmatters.poom.ci.apps.releaser.task.ReleaseTask;
import org.codingmatters.poom.ci.apps.releaser.task.ReleaseTaskResult;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIRequesterClient;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Arguments;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class App {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(App.class);

    /**
     * mvn exec:java -Dexec.mainClass=org.codingmatters.poom.ci.apps.releaser.App -Dexec.args="release-graph /tmp/playground/graph.yml"
     * @param args
     */
    public static void main(String[] args) {
        Arguments arguments = Arguments.from(args);

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

        PoomCIPipelineAPIClient client = new PoomCIPipelineAPIRequesterClient(
                new OkHttpRequesterFactory(OkHttpClientWrapper.build(), () -> pipelineUrl),
                jsonFactory,
                pipelineUrl
        );


        if(arguments.arguments().get(0).equals("release")) {
            Arguments.OptionValue repository = arguments.option("repository");
            if(! repository.isPresent()) {
                usageAndFail(args);
            }
            if(repository.get() == null) {
                usageAndFail(args);
            }

            try {
                ReleaseTaskResult result = new ReleaseTask(repository.get(), commandHelper, client).call();
                if(result.exitStatus().equals(ReleaseTaskResult.ExitStatus.SUCCESS)) {
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
        } else if(arguments.arguments().get(0).equals("release-graph")) {
            if(arguments.argumentCount() < 2) {
                usageAndFail(args);
            }
            try {
                RepositoryGraphDescriptor descriptor;
                try (InputStream in = new FileInputStream(arguments.arguments().get(1))) {
                    descriptor = RepositoryGraphDescriptor.fromYaml(in);
                }
                System.out.println("Will release dependency graph : " + descriptor.graph());
                ExecutorService pool = Executors.newFixedThreadPool(10);
                GraphWalker releaseWalker = new GraphWalker(
                        descriptor,
                        (repository, context) -> new ReleaseTask(repository, context, commandHelper, client),
                        pool
                );
                GraphWalkResult result = pool.submit(releaseWalker).get();
                if(result.exitStatus().equals(ReleaseTaskResult.ExitStatus.SUCCESS)) {
                    System.out.println(result.message());
                    System.exit(0);
                } else {
                    System.err.println(result.message());
                    System.exit(2);
                }
            } catch (Exception e) {
                log.error("failed executing release-graph", e);
                System.exit(3);
            }
        } else {
            usageAndFail(args);
        }


    }

    private static void usageAndFail(String[] args) {
        usage(System.err, args);
        System.exit(1);
    }

    private static void usage(PrintStream where, String[] args) {
        where.println("Called with : " + args == null ? "" : Arrays.stream(args).collect(Collectors.joining(" ")));
        where.println("   help");
        where.println("      prints this usage message");
        where.println("   release --repository <repository, i.e. flexiooss/poom-ci>");
        where.println("      releases the repository and waits for the build pipeline to finish");
        where.println("   release-graph <graph file>");
        where.println("      releases a complete repository graph");
    }
}
