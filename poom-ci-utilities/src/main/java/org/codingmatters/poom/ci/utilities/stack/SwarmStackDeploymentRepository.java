package org.codingmatters.poom.ci.utilities.stack;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.utilities.stack.descriptors.AnsiblePlaybook;
import org.codingmatters.poom.ci.utilities.stack.descriptors.ServiceDescriptor;
import org.codingmatters.poom.ci.utilities.stack.descriptors.StackDescriptor;
import org.codingmatters.poom.ci.utilities.stack.descriptors.ValueList;
import org.codingmatters.poom.ci.utilities.stack.descriptors.json.AnsiblePlaybookReader;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwarmStackDeploymentRepository {

    static private YAMLFactory factory = new YAMLFactory();

    private final File root;
    private final File mainYaml;

    public SwarmStackDeploymentRepository(File root) {
        this.root = root;
        this.mainYaml = new File(this.root, "main.yml");
    }

    public StackDescriptor buildStackDescriptor() throws IOException {
        AnsiblePlaybook[] playbooks = this.readStackPlaybooks();
        for (AnsiblePlaybook playbook : playbooks) {
            for(String role : playbook.opt().roles().orElseGet(() -> ValueList.<String>builder().build())) {
                File stackTemplate = new File(this.root, "roles/" + role + "/templates/stack.yml.j2");
                if(stackTemplate.exists()) {
                    String stackName = playbook.vars().property("stack_name").single().stringValue();
                    List<ServiceDescriptor> services = this.stackServiceDescriptors(stackTemplate);
                    return StackDescriptor.builder()
                            .name(stackName)
                            .services(services)
                            .build();
                }
            }
        }
        return null;
    }

    private AnsiblePlaybook[] readStackPlaybooks() throws IOException {
        List<AnsiblePlaybook> results = new LinkedList<>();
        try(JsonParser parser = factory.createParser(this.mainYaml)) {
            AnsiblePlaybook[] palybooks = new AnsiblePlaybookReader().readArray(parser);
            for (AnsiblePlaybook palybook : palybooks) {
                if(palybook.vars().has("stack_name")) {
                    results.add(palybook);
                }
            }
        }
        return results.toArray(new AnsiblePlaybook[0]);
    }

    private List<ServiceDescriptor> stackServiceDescriptors(File stackTemplate) throws IOException {
        String content = this.cleanup(stackTemplate);

        Map<String, StringBuilder> serviceCandidateChunks = new LinkedHashMap<>();
        try(BufferedReader reader = new BufferedReader(new StringReader(content))) {
            this.readUntil(reader, Pattern.compile("services:\\s+$"));
            Pattern firstServicePattern = Pattern.compile("^(\\s+)([A-Za-z-_]+):\\s*$");
            String firstService = this.readUntil(reader, firstServicePattern);

            Matcher firstServiceMatcher = firstServicePattern.matcher(firstService);
            firstServiceMatcher.matches();
            String indent = firstServiceMatcher.group(1);
            String service = firstServiceMatcher.group(2);
            serviceCandidateChunks.put(service, new StringBuilder());

            Pattern servicePattern = Pattern.compile(indent + "([A-Za-z-_]+):\\s*$");
            for (String line = reader.readLine() ; line != null ; line = reader.readLine()) {
                Matcher serviceMatcher = servicePattern.matcher(line);
                if(serviceMatcher.matches()) {
                    service = serviceMatcher.group(1);
                    serviceCandidateChunks.put(service, new StringBuilder());
                } else {
                    serviceCandidateChunks.get(service).append(line).append("\n");
                }
            }
        }

        List<ServiceDescriptor> result = new LinkedList<>();
        for (String service : serviceCandidateChunks.keySet()) {
            Pattern imagePattern = Pattern.compile("\\s*image:\\s*([^\\s]+)\\s*");
            Matcher imageMatcher = imagePattern.matcher(serviceCandidateChunks.get(service));
            if(imageMatcher.find()) {
                String image = this.parseImage(imageMatcher.group(1));
                result.add(ServiceDescriptor.builder().name(service).image(image).build());
            }
        }
        return result;
    }

    private String parseImage(String from) {
        /*
        ${AUDIT_FLEXIO_REGISTRY:-harbor.ci.flexio.io}/flexio/flexio-usage-audit-service:
        -> harbor.ci.flexio.io/flexio/flexio-usage-audit-service
         */
        Matcher fullImageWithREGISTRYVarriableMatcher = Pattern.compile("\\$\\{[^:]+:-([^}]+)}(.*):").matcher(from);
        if(fullImageWithREGISTRYVarriableMatcher.matches()) {
            return fullImageWithREGISTRYVarriableMatcher.group(1) + fullImageWithREGISTRYVarriableMatcher.group(2);
        } else {
            /*
            grafana/grafana: -> grafana/grafana
             */
            if(from.endsWith(":")) {
                return from.substring(0, from.length() - 1);
            } else {
                return from;
            }
        }
    }

    private String readUntil(BufferedReader reader, Pattern pattern) throws IOException {
        String line = reader.readLine();
        while(line != null && ! pattern.matcher(line).matches()) {
            line = reader.readLine();
        }
        return line;
    }

    private String cleanup(File file) throws IOException {
        String result = this.fileContent(file);

        Matcher directiveMatcher = Pattern.compile("(?>\\{%).*(?>%})", Pattern.MULTILINE).matcher(result);
        result = directiveMatcher.replaceAll("");

        Matcher templateMatcher = Pattern.compile("(?>\\{\\{).*(?>}})", Pattern.MULTILINE).matcher(result);
        result = templateMatcher.replaceAll("");
        return result;
    }

    private String fileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try(Reader reader = new FileReader(file)) {
            char[] buffer = new char[1024];
            for(int read = reader.read(buffer) ; read != -1 ; read = reader.read(buffer)) {
                content.append(buffer, 0, read);
            }
        }
        return content.toString();
    }
}
