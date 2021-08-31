package org.codingmatters.poom.ci.utilities.stack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.utilities.stack.descriptors.ImageDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DockerComposeBuild {

    static public DockerComposeBuild[] lookup(File root) throws IOException {
        List<DockerComposeBuild> result = new LinkedList<>();
        lookupTo(root, result);
        return result.toArray(new DockerComposeBuild[0]);
    }

    private static void lookupTo(File file, List<DockerComposeBuild> result) throws IOException{
        if(file.isDirectory()) {
            for (File child : file.listFiles()) {
                lookupTo(child, result);
            }
        } else if("docker-compose-build.yml".equals(file.getName())) {
            try(InputStream in = new FileInputStream(file)) {
                result.add(new DockerComposeBuild(in));
            }
        }
    }

    static private YAMLFactory factory = new YAMLFactory();
    private final Map<String, Object> descriptor;

    public DockerComposeBuild(InputStream composeStream) throws IOException {
        this.descriptor = new ObjectMapper(factory).readValue(composeStream, Map.class);
    }

    public ImageDescriptor[] builds() {
        if(this.descriptor.containsKey("services")) {
            if(this.descriptor.get("services") instanceof Map) {
                List<ImageDescriptor> results = new LinkedList<>();
                for (Object service : ((Map) this.descriptor.get("services")).values()) {
                    if(service instanceof Map) {
                        if(((Map)service).containsKey("image")) {
                            String image = ((Map)service).get("image").toString();
                            int colonIndex = image.lastIndexOf(':');
                            if(colonIndex != -1) {
                                results.add(ImageDescriptor.builder().image(image.substring(0, colonIndex)).version(image.substring(colonIndex + 1)).build());
                            }
                        }
                    }
                }
                return results.toArray(new ImageDescriptor[0]);
            }
        }
        return new ImageDescriptor[0];
    }
}
