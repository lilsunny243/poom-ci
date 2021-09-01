package org.codingmatters.poom.ci.apps.notifiers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.codingmatters.poom.ci.apps.notifiers.notification.data.StackServiceNotification;
import org.codingmatters.poom.ci.apps.notifiers.notification.data.json.StackServiceNotificationWriter;
import org.codingmatters.poom.ci.apps.utils.GenericNotifier;
import org.codingmatters.poom.ci.utilities.stack.SwarmStackDeploymentRepository;
import org.codingmatters.poom.ci.utilities.stack.descriptors.ServiceDescriptor;
import org.codingmatters.poom.ci.utilities.stack.descriptors.StackDescriptor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Arguments;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class StackDeploymentNotifier {
    static private CategorizedLogger log = CategorizedLogger.getLogger(StackDeploymentNotifier.class);

    /**
     *
     * mvn exec:java -Dexec.mainClass=org.codingmatters.poom.ci.apps.notifiers.StackDeploymentNotifier -Dexec.args="/home/nel/workspaces/deployments/deployment-audit --repository Flexio-corp/deployment-audit --version 123456 --url https://api.flexio.io/httpin/support/in/612caada0a228d695c2a1411 --bearer cdb48010-3508-4a01-b619-3a9c3cf9164a"
     *
     * @param args
     */
    public static void main(String[] args) {
        Arguments arguments = Arguments.from(args);

        if(arguments.argumentCount() < 1) throw usage("missing project dir");
        File projectDir = new File(arguments.arguments().get(0));
        if(! projectDir.exists()) throw usage("project dir doesn't exist");

        if(! arguments.option("repository").isPresent()) throw usage("missing repository");
        if(! arguments.option("version").isPresent()) throw usage("missing version");
        if(! arguments.option("url").isPresent()) throw usage("missing notification url");

        try {
            new StackDeploymentNotifier(
                    projectDir,
                    arguments.option("repository").get(),
                    arguments.option("version").get(),
                    new JsonFactory(),
                    OkHttpClientWrapper.build(),
                    arguments.option("url").get(),
                    arguments.option("bearer").isPresent() ? arguments.option("bearer").get() : null
            ).notifyServices();
        } catch (IOException e) {
            log.error("error notifying with " + arguments, e);
            System.exit(1);
        }
    }

    private static RuntimeException usage(String message) {
        log.error("usage : <project dir> --repository <repository> --url <notification url> --bearer <bearer token>");
        log.error("\t" + message);
        return new RuntimeException("usage : <project dir> --repository <repository> --url <notification url> --bearer <bearer token>\n\t" + message);
    }

    private final File projectDir;
    private final String repository;
    private final String version;
    private final JsonFactory jsonFactory;
    private final GenericNotifier notifier;

    public StackDeploymentNotifier(File projectDir, String repository, String version, JsonFactory jsonFactory, HttpClientWrapper wrapper, String notificationUrl, String notificationBearer) {
        this.projectDir = projectDir;
        this.repository = repository;
        this.version = version;
        this.jsonFactory = jsonFactory;

        this.notifier = new GenericNotifier(wrapper, notificationUrl, notificationBearer);
    }

    public void notifyServices() throws IOException {
        StackDescriptor stack = new SwarmStackDeploymentRepository(this.projectDir).buildStackDescriptor();
        if(stack != null) {
            for (ServiceDescriptor service : stack.services()) {
                this.notifyService(notification -> notification
                        .repository(this.repository)
                        .stack(stack.name())
                        .version(this.version)
                        .service(service.name())
                        .image(service.image())
                );
            }
        } else {
            log.info("no stack found");
        }
    }

    private void notifyService(StackServiceNotification.Changer config) throws IOException {
        StackServiceNotification notification = config.configure(StackServiceNotification.builder()).build();
        String json;
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(); JsonGenerator generator = this.jsonFactory.createGenerator(out)) {
            new StackServiceNotificationWriter().write(generator, notification);
            generator.close();
            json = out.toString();
        }
        this.notifier.notify(json);
    }
}
