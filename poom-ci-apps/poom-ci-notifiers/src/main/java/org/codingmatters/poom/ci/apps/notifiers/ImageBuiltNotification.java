package org.codingmatters.poom.ci.apps.notifiers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.codingmatters.poom.ci.apps.notifiers.notification.data.ImageNotification;
import org.codingmatters.poom.ci.apps.notifiers.notification.data.json.ImageNotificationWriter;
import org.codingmatters.poom.ci.apps.utils.GenericNotifier;
import org.codingmatters.poom.ci.utilities.stack.DockerComposeBuild;
import org.codingmatters.poom.ci.utilities.stack.descriptors.ImageDescriptor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Arguments;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class ImageBuiltNotification {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ImageBuiltNotification.class);

    /**
     *
     * mvn exec:java -Dexec.mainClass=org.codingmatters.poom.ci.apps.notifiers.ImageBuiltNotification -Dexec.args="/home/nel/workspaces/flexio-bundles/dashboard --repository Flexio-corp/deployment-audit --url https://api.flexio.io/httpin/support/in/612db9ee0a228d695c2a1594 --bearer 54c77b54-9c65-4caf-87ba-c5887ac3ffd4"
     *
     * @param args
     */
    public static void main(String[] args) {
        Arguments arguments = Arguments.from(args);

        if(arguments.argumentCount() < 1) throw usage("missing project dir");
        File projectDir = new File(arguments.arguments().get(0));
        if(! projectDir.exists()) throw usage("project dir doesn't exist");
        if(! arguments.option("url").isPresent()) throw usage("missing notification url");

        try {
            new ImageBuiltNotification(
                    projectDir,
                    new JsonFactory(),
                    OkHttpClientWrapper.build(),
                    arguments.option("url").get(),
                    arguments.option("bearer").isPresent() ? arguments.option("bearer").get() : null
            ).notifyImages();
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
    private final JsonFactory jsonFactory;
    private final GenericNotifier notifier;

    public ImageBuiltNotification(File projectDir, JsonFactory jsonFactory, HttpClientWrapper wrapper, String notificationUrl, String notificationBearer) {
        this.projectDir = projectDir;
        this.jsonFactory = jsonFactory;
        this.notifier = new GenericNotifier(wrapper, notificationUrl, notificationBearer);
    }

    private void notifyImages() throws IOException {
        for (DockerComposeBuild dockerComposeBuild : DockerComposeBuild.lookup(this.projectDir)) {
            for (ImageDescriptor image : dockerComposeBuild.builds()) {
                this.notifyImage(notification -> notification.image(image.image()).version(image.version()));
            }
        }
    }

    private void notifyImage(ImageNotification.Changer config) throws IOException {
        ImageNotification notification = config.configure(ImageNotification.builder()).build();
        String json;
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(); JsonGenerator generator = this.jsonFactory.createGenerator(out)) {
            new ImageNotificationWriter().write(generator, notification);
            generator.close();
            json = out.toString();
        }
        this.notifier.notify(json);
    }
}
