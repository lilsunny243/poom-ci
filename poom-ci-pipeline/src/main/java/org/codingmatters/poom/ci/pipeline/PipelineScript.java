package org.codingmatters.poom.ci.pipeline;

import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.pipeline.descriptors.Stage;
import org.codingmatters.poom.ci.pipeline.descriptors.optional.OptionalStage;

import java.io.IOException;
import java.io.OutputStream;

public class PipelineScript {

    private final Pipeline pipeline;

    public PipelineScript(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    public void forStage(String stageName, OutputStream out) throws IOException {
        Stage stage = this.theStage(stageName).orElseThrow(() -> new IOException("no such stage " + stageName));

        this.header(out);
        this.env(out);
        this.stage(stage, out);
    }

    private OptionalStage theStage(String named) {
        for (Stage stage : this.pipeline.stages()) {
            if(named.equals(stage.name())) {
                return stage.opt();
            }
        }
        return OptionalStage.of(null);
    }

    private void header(OutputStream out) throws IOException {
        String header =
        "#!/usr/bin/env bash\n" +
        "\n" +
        "if [[ $# -eq 0 ]] ; then\n" +
        "    echo 'must provide a workspace as argument'\n" +
        "    exit 1\n" +
        "fi\n" +
        "\n" +
        "WORKSPACE=$1\n" +
        "SRC=$(dirname $(readlink -f $0))\n" +
        "mkdir -p $WORKSPACE/M2\n" +
        "\n" +
        "rm -rf $WORKSPACE/logs\n" +
        "mkdir -p $WORKSPACE/logs\n\n";

        out.write(header.getBytes());
    }

    private void env(OutputStream out) throws IOException {
        for (String variable : this.pipeline.env().propertyNames()) {
            String variableLine = String.format("%s=\"%s\"\n", variable, this.pipeline.env().property(variable).single().stringValue());
            out.write(variableLine.getBytes());
        }
        out.write("\n".getBytes());
    }

    private void stage(Stage stage, OutputStream out) throws IOException {
        this.stageVars(stage, out);
        this.exec(stage, out);
        this.stageResult(stage, out);
    }

    private void stageVars(Stage stage, OutputStream out) throws IOException {
        String vars = String.format(
                "STAGE=%s\n" +
                "STAGE_OUT=$WORKSPACE/logs/$STAGE.stdout.log\n" +
                "STAGE_ERR=$WORKSPACE/logs/$STAGE.stderr.log\n\n",
                stage.name()
        );

        out.write(vars.getBytes());
    }

    private void exec(Stage stage, OutputStream out) throws IOException {
        int i = 0;
        for (String exec : stage.exec()) {
            i++;
            String call = String.format(
            "%s > >(tee -a $STAGE_OUT) 2> >(tee -a $STAGE_ERR >&2)\n" +
            "RESULT=$?\n" +
            "if [ \"$RESULT\" -ne 0 ]\n" +
            "then\n" +
            "    echo \"stage $STAGE exec %s failure\"\n" +
            "    exit $RESULT\n" +
            "fi\n",
            exec, i
            );

            out.write(call.getBytes());
        }
        out.write("\n".getBytes());
    }

    private void stageResult(Stage stage, OutputStream out) throws IOException {
        String result =
                "echo \"$STAGE STAGE EXIT : $RESULT\"\n" +
                        "exit $RESULT";

        out.write(result.getBytes());
    }
}
