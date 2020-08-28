package org.codingmatters.poom.ci.apps.releaser.command.exception;

public class CommandFailed extends Exception {
    public CommandFailed(String message) {
        super(message);
    }

    public CommandFailed(String message, Throwable cause) {
        super(message, cause);
    }
}
