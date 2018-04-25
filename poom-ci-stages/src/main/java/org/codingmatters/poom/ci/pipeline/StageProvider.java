package org.codingmatters.poom.ci.pipeline;

import org.codingmatters.poom.ci.pipeline.descriptors.Stage;
import org.codingmatters.poom.ci.pipeline.descriptors.StageHolder;
import org.codingmatters.poom.ci.pipeline.descriptors.ValueList;

public interface StageProvider {
    ValueList<Stage> stages();
    ValueList<Stage> onSuccess();
    ValueList<Stage> onError();

    default Stage stage(String name) {
        return this.named(this.stages(), name);
    }

    default StageHolder stageHolder(String name) {
        return this.holder(StageHolder.Type.MAIN, this.stage(name));
    }

    default StageHolder[] stageHolders() {
        return this.holders(StageHolder.Type.MAIN, this.stages());
    }



    default Stage onSuccess(String name) {
        return this.named(this.onSuccess(), name);
    }

    default StageHolder onSuccessHolder(String name) {
        return this.holder(StageHolder.Type.SUCCESS, this.onSuccess(name));
    }

    default StageHolder[] onSuccessHolders() {
        return this.holders(StageHolder.Type.SUCCESS, this.onSuccess());
    }




    default Stage onError(String name) {
        return this.named(this.onError(), name);
    }

    default StageHolder onErrorHolder(String name) {
        return this.holder(StageHolder.Type.ERROR, this.onError(name));
    }

    default StageHolder[] onErrorHolders() {
        return this.holders(StageHolder.Type.ERROR, this.onError());
    }



    default Stage named(ValueList<Stage> stages, String name) {
        return stages.stream().filter(stage -> stage.name().equals(name)).findFirst().orElse(null);
    }

    default StageHolder holder(StageHolder.Type type, Stage stage) {
        return StageHolder.builder().type(type).stage(stage).build();
    }

    default StageHolder[] holders(StageHolder.Type type, ValueList<Stage> stages) {
        return stages.stream().map(stage -> this.holder(type, stage)).toArray(i -> new StageHolder[i]);
    }

}
