package org.codingmatters.poom.ci.runners.pipeline;

public class NotAPipelineContextException extends Exception {
    public NotAPipelineContextException(String s) {
        super(s);
    }
}
