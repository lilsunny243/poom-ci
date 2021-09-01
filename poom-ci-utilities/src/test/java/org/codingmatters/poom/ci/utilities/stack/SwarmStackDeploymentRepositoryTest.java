package org.codingmatters.poom.ci.utilities.stack;

import org.codingmatters.poom.ci.utilities.stack.descriptors.ServiceDescriptor;
import org.codingmatters.poom.ci.utilities.stack.descriptors.StackDescriptor;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SwarmStackDeploymentRepositoryTest {

    @Rule
    public TemporaryFolder folder= new TemporaryFolder();



    @Test
    public void audit() throws Exception {
        this.importZipResource("deployment-audit.zip");
        File deployment = new File(this.folder.getRoot(), "deployment-audit");

        StackDescriptor stack = new SwarmStackDeploymentRepository(deployment).buildStackDescriptor();

        assertThat(
                stack,
                is(StackDescriptor.builder()
                        .name("audit")
                        .services(
                                ServiceDescriptor.builder().name("audit").image("harbor.ci.flexio.io/flexio/flexio-usage-audit-service").build(),
                                ServiceDescriptor.builder().name("audit-connector").image("harbor.ci.flexio.io/flexio/flexio-usage-audit-connector").build(),
                                ServiceDescriptor.builder().name("audit-runner").image("harbor.ci.flexio.io/flexio/flexio-usage-audit-runner").build()
                        )
                        .build())
        );
    }

    @Test
    public void sslGateway() throws Exception {
        this.importZipResource("deployment-ssl-gateway.zip");
        File deployment = new File(this.folder.getRoot(), "deployment-ssl-gateway");

        StackDescriptor stack = new SwarmStackDeploymentRepository(deployment).buildStackDescriptor();

        assertThat(
                stack,
                is(StackDescriptor.builder()
                        .name("gateway")
                        .services(
                                ServiceDescriptor.builder().name("gateway").image("harbor.ci.flexio.io/flexio/ssl-gateway").build()
                        )
                        .build())
        );
    }

    @Test
    public void metricsInfra() throws Exception {
        this.importZipResource("deployment-metrics-infra.zip");
        File deployment = new File(this.folder.getRoot(), "deployment-metrics-infra");

        StackDescriptor stack = new SwarmStackDeploymentRepository(deployment).buildStackDescriptor();

        assertThat(
                stack,
                is(StackDescriptor.builder()
                        .name("metrics-infra")
                        .services(
                                ServiceDescriptor.builder().name("metrics-gateway").image("nginx").build(),
                                ServiceDescriptor.builder().name("metrics").image("petergrace/opentsdb-docker").build(),
                                ServiceDescriptor.builder().name("grafana").image("grafana/grafana").build(),
                                ServiceDescriptor.builder().name("victoria").image("harbor.ci.flexio.io/flexio/victoria-metrics").build(),
                                ServiceDescriptor.builder().name("graf").image("grafana/grafana").build()
                        )
                        .build())
        );
    }

    @Test
    public void deploymentWidgetUi() throws Exception {
        this.importZipResource("deployment-widget-ui.zip");
        File deployment = new File(this.folder.getRoot(), "deployment-widget-ui");

        StackDescriptor stack = new SwarmStackDeploymentRepository(deployment).buildStackDescriptor();

        assertThat(
                stack,
                is(StackDescriptor.builder()
                        .name("widget-ui")
                        .services(
                                ServiceDescriptor.builder().name("flexio-widget-ui").image("harbor.ci.flexio.io/flexio/flexio-widget-client-ui").build()
                        )
                        .build())
        );
    }

    @Test
    public void deploymentCi() throws Exception {
        this.importZipResource("deployment-ci.zip");
        File deployment = new File(this.folder.getRoot(), "deployment-ci");

        StackDescriptor stack = new SwarmStackDeploymentRepository(deployment).buildStackDescriptor();

        assertThat(
                stack,
                is(StackDescriptor.builder()
                        .name("ci")
                        .services(
                                ServiceDescriptor.builder().name("github-webhook").image("codingmatters/poom-ci-github-webhook").build(),
                                ServiceDescriptor.builder().name("pipelines").image("codingmatters/poom-ci-pipeline-service").build(),
                                ServiceDescriptor.builder().name("dependencies").image("codingmatters/poom-ci-dependency-flat-service").build(),
                                ServiceDescriptor.builder().name("jobs").image("codingmatters/poom-ci-service-bundle").build(),
                                ServiceDescriptor.builder().name("maven-repository").image("codingmatters/archiva").build(),
                                ServiceDescriptor.builder().name("mongo").image("mongo").build(),
                                ServiceDescriptor.builder().name("poom-pack").image("codingmatters/poom-pack-composer-service").build(),
                                ServiceDescriptor.builder().name("verdaccio").image("codingmatters/poom-verdaccio").build(),
                                ServiceDescriptor.builder().name("flexio-ci-client").image("harbor.ci.flexio.io/flexio/flexio-ci-client").build()
                        )
                        .build())
        );
    }

    @Test
    public void deploymentKeystore() throws Exception {
        this.importZipResource("deployment-keystore.zip");
        File deployment = new File(this.folder.getRoot(), "deployment-keystore");

        StackDescriptor stack = new SwarmStackDeploymentRepository(deployment).buildStackDescriptor();

        assertThat(stack, is(nullValue()));
    }


    private void importZipResource(String zipResource) throws IOException {
        File zipTemporary = this.folder.newFile(zipResource);
        try(
                InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(zipResource);
                OutputStream out = new FileOutputStream(zipTemporary)
        ) {
            byte[] buffer = new byte[1024];
            for(int read = in.read(buffer) ; read != -1 ; read = in.read(buffer)) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }

        ZipFile zipFile = new ZipFile(zipTemporary);

        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while(entries.hasMoreElements()){
            ZipEntry entry = entries.nextElement();
            if(! entry.isDirectory()) {
                File entryFile = new File(this.folder.getRoot(), entry.getName());
                entryFile.getParentFile().mkdirs();
                try(
                        InputStream in = zipFile.getInputStream(entry);
                        OutputStream out = new FileOutputStream(entryFile)
                ) {
                    byte[] buffer = new byte[1024];
                    for(int read = in.read(buffer) ; read != -1 ; read = in.read(buffer)) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                }
            }
        }
        zipTemporary.delete();
    }

    private void print(File file, String prefix) {
        if(file.isDirectory()) {
            System.out.println(prefix + "+ " + file.getName());
            for (File child : file.listFiles()) {
                this.print(child, prefix + "   ");
            }
        } else {
            System.out.println(prefix + file.getName());
        }
    }
}