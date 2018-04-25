package org.codingmatters.poom.ci.pipeline;

import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.pipeline.descriptors.Stage;
import org.codingmatters.poom.ci.pipeline.descriptors.StageHolder;

import java.io.IOException;
import java.io.OutputStream;

public class PipelineScript {

    private final Pipeline pipeline;

    public PipelineScript(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    public void forStage(StageHolder stg, OutputStream out) throws IOException {
        Stage stage = stg.stage();
        this.header(out);
        this.env(out);
        this.stage(stage, out);
        this.stageResult(stage, out);
    }

    public void forPipeline(OutputStream out) throws IOException {
        this.header(out);
        this.env(out);
        for (Stage stage : this.pipeline.stages()) {
            this.stage(stage, out);
        }
        this.pipelineResult(out);
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
        "if [[ $# -gt 1 ]] ; then\n" +
        "    SRC=$(readlink -f $2)\n" +
        "    echo \"running $0 on $SRC\"\n" +
        "fi\n" +
        "\n" +
        "rm -rf $WORKSPACE/logs\n" +
        "mkdir -p $WORKSPACE/logs\n\n";

        out.write(header.getBytes());
    }

    private void env(OutputStream out) throws IOException {
        System.out.println(this.pipeline.env());
        if(this.pipeline.env() != null && this.pipeline.env().propertyNames() != null) {
            for (String variable : this.pipeline.env().propertyNames()) {
                String variableLine = String.format("%s=\"%s\"\n", variable, this.pipeline.env().property(variable).single().stringValue());
                out.write(variableLine.getBytes());
            }
            out.write("\n".getBytes());
        }
    }

    private void stage(Stage stage, OutputStream out) throws IOException {
        this.stageVars(stage, out);
        this.exec(stage, out);
    }

    private void stageVars(Stage stage, OutputStream out) throws IOException {
        String vars = String.format(
                "STAGE=%s\n" + "\n",
//                "STAGE_OUT=$WORKSPACE/logs/$STAGE.stdout.log\n" +
//                "STAGE_ERR=$WORKSPACE/logs/$STAGE.stderr.log\n\n",
                stage.name()
        );

        out.write(vars.getBytes());
    }

    private void exec(Stage stage, OutputStream out) throws IOException {
        int i = 0;
        for (String exec : stage.exec()) {
            i++;
            String call = String.format(
//            "%s > >(tee -a $STAGE_OUT) 2> >(tee -a $STAGE_ERR >&2)\n" +
            "%s\n" +
            "RESULT=$?\n" +
            "if [ \"$RESULT\" -ne 0 ]\n" +
            "then\n" +
            "    echo \"stage $STAGE exec %s failure\"\n" +
            "    exit $RESULT\n" +
            "fi\n\n",
            exec, i
            );

            out.write(call.getBytes());
        }
    }

    private void stageResult(Stage stage, OutputStream out) throws IOException {
        String result =
        "echo \"$STAGE STAGE EXIT : $RESULT\"\n" +
        "exit $RESULT";

        out.write(result.getBytes());
    }


    private void pipelineResult(OutputStream out) throws IOException {
        String result =
        "echo \"PIPELINE EXIT : $RESULT\"\n" +
        "exit $RESULT";

        out.write(result.getBytes());
    }
}
