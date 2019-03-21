package org.codingmatters.poom.ci.pipeline.merge;

import org.codingmatters.poom.ci.pipeline.PipelineMerger;
import org.codingmatters.poom.ci.pipeline.descriptors.Pipeline;
import org.codingmatters.poom.ci.pipeline.descriptors.Secret;
import org.codingmatters.poom.ci.pipeline.descriptors.Stage;
import org.codingmatters.poom.ci.pipeline.descriptors.ValueList;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class SimplePipelineMerger implements PipelineMerger {
    @Override
    public Pipeline merge(Pipeline pipeline, Pipeline into) {
        Pipeline.Builder result = Pipeline.builder();

        result.stages(this.mergeStages(pipeline.stages(), into.stages()));
        result.cleanup(this.mergeStages(pipeline.cleanup(), into.cleanup()));
        result.onSuccess(this.mergeStages(pipeline.onSuccess(), into.onSuccess()));
        result.onError(this.mergeStages(pipeline.onError(), into.onError()));

        result.env(this.mergeEnv(pipeline.env(), into.env()));
        result.secrets(this.mergeSecrets(pipeline.secrets(), into.secrets()));

        return result.build();
    }

    private Collection<Stage> mergeStages(ValueList<Stage> stages, ValueList<Stage> into) {
        List<Stage> result = new LinkedList<>();

        LinkedList<String> stageIndex = new LinkedList<>();

        if(into != null) {
            for (Stage stage : into) {
                result.add(stage);
                stageIndex.add(stage.name());
            }
        }

        if(stages != null) {
            for (Stage stage : stages) {
                if(stage.opt().before().isPresent() && stageIndex.contains(stage.before())) {
                    int baseIndex = stageIndex.indexOf(stage.before());
                    stageIndex.add(baseIndex, stage.name());
                    result.add(baseIndex, this.cleaned(stage));
                } else if(stage.opt().after().isPresent() && stageIndex.contains(stage.after())) {
                    int baseIndex = stageIndex.indexOf(stage.after());
                    stageIndex.add(baseIndex + 1, stage.name());
                    result.add(baseIndex + 1, this.cleaned(stage));
                } else {
                    result.add(this.cleaned(stage));
                }
            }
        } else if(into == null){
            return null;
        }
        return result;
    }

    private Stage cleaned(Stage stage) {
        return stage
                .withBefore(null)
                .withAfter(null);
    }

    private ObjectValue[] mergeEnv(ValueList<ObjectValue> env, ValueList<ObjectValue> into) {
        List<ObjectValue> result = new LinkedList<>();
        if(into != null) {
            for (ObjectValue objectValue : into) {
                result.add(objectValue);
            }
        }
        if(env != null) {
            for (ObjectValue objectValue : env) {
                result.add(objectValue);
            }
        }
        if(into == null && result.isEmpty()) {
            return null;
        } else {
            return result.toArray(new ObjectValue[0]);
        }
    }

    private Secret[] mergeSecrets(ValueList<Secret> secrets, ValueList<Secret> into) {
        List<Secret> result = new LinkedList<>();

        HashMap<String, Integer> secretIndex = new HashMap<>();

        if(into != null) {
            for (Secret secret : into) {
                result.add(secret);
                secretIndex.put(secret.name(), result.size() - 1);
            }
        }
        if(secrets != null) {
            for (Secret secret : secrets) {
                if(secretIndex.containsKey(secret.name())) {
                    result.set(secretIndex.get(secret.name()), secret);
                } else {
                    result.add(secret);
                    secretIndex.put(secret.name(), result.size() - 1);
                }
            }
        }

        if(into == null && result.isEmpty()) {
            return null;
        } else {
            return result.toArray(new Secret[0]);
        }
    }
}
