package org.codingmatters.poom.ci.pipeline.stage;

public class OnlyWhenParsingException extends Exception {
    public OnlyWhenParsingException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
