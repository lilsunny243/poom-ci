package org.codingmatters.poom.ci.apps.releaser;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.apps.releaser.command.CommandHelper;
import org.codingmatters.poom.ci.apps.releaser.task.ReleaseTask;
import org.codingmatters.poom.ci.apps.releaser.task.TaskResult;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIRequesterClient;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Arguments;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

import java.io.PrintStream;

public class App {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(App.class);

    public static void main(String[] args) {
        Arguments arguments = Arguments.from(args);

        if(arguments.argumentCount() < 1) {
            usageAndFail();
        }

        if(arguments.arguments().get(0).equals("help")) {
            usage(System.out);
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
                usageAndFail();
            }
            if(repository.get() == null) {
                usageAndFail();
            }
            String repositoryUrl = String.format("git@github.com:%s.git", repository.get());

            try {
                TaskResult result = new ReleaseTask(repositoryUrl, repository.get(), commandHelper, client).call();
                if(result.exitStatus().equals(TaskResult.ExitStatus.SUCCESS)) {
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
        }


    }

    private static void usageAndFail() {
        usage(System.err);
        System.exit(1);
    }

    private static void usage(PrintStream where) {
        where.println("Usage : <command>");
        where.println("   help");
        where.println("      prints this usage message");
        where.println("   release --repository <repository, i.e. flexiooss/poom-ci>");
        where.println("      releases the repository and waits for the build pipeline to finish");
    }
}
