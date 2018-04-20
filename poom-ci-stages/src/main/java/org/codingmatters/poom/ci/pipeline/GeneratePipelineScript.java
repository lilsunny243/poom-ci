package org.codingmatters.poom.ci.pipeline;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.pipeline.descriptors.json.PipelineReader;

import java.io.*;

public class GeneratePipelineScript {
    public static void main(String[] args) {
        if(args.length < 1) {
            System.err.println("usage : <directory with poom-ci-pipeline.yaml in it>");
            System.exit(1);
        }
        File home = new File(args[0]);

        Pipeline pipeline = null;

        PipelineReader reader = new PipelineReader();
        YAMLFactory factory = new YAMLFactory();
        try(InputStream in = new FileInputStream(new File(home, "poom-ci-pipeline.yaml")); JsonParser parser = factory.createParser(in)) {
            pipeline = reader.read(parser);
        } catch(IOException e) {
            e.printStackTrace();
            System.err.println("couldn't read pipeline !");
            System.exit(2);
        }

        PipelineScript pipelineScript = new PipelineScript(pipeline);

        File mainScript = null;
        try {
            mainScript = createScriptFile(home, "poom-ci-pipeline.sh");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("couldn't create pipeline script!");
            System.exit(3);
        }
        try(OutputStream out = new FileOutputStream(mainScript)) {
            pipelineScript.forPipeline(out);
        } catch(IOException e) {
            e.printStackTrace();
            System.err.println("couldn't generate pipeline script !");
            System.exit(4);
        }


        for (int i = 1; i < args.length; i++) {
            String stage = args[i];

            File stageScript = null;
            try {
                stageScript = createScriptFile(home, String.format("poom-ci-%s.sh", stage));
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("couldn't create " + stage + " script!");
                System.exit(3);
            }
            try(OutputStream out = new FileOutputStream(stageScript)) {
                pipelineScript.forPipeline(out);
            } catch(IOException e) {
                e.printStackTrace();
                System.err.println("couldn't generate " + stage + " script !");
                System.exit(4);
            }
        }
    }

    private static File createScriptFile(File dir, String name) throws IOException {
        File result = new File(dir, name);
        if(result.exists()) {
            result.delete();
        }
        result.createNewFile();
        result.setExecutable(true);

        return result;
    }
}
