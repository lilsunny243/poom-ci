package org.codingmatters.poom.ci.pipeline.api.service.helpers;

import org.codingmatters.poom.ci.pipeline.api.types.Stage;

public class StageHelper {
    static public boolean isStageTypeValid(String stageType) {
        if(stageType == null || stageType.isEmpty()) return false;
        try {
            Stage.StageType.valueOf(stageType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }
}
