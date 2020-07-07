package org.codingmatters.poom.ci.dependency.flat;

public class NoSuchRepositoryException extends Exception {
    public NoSuchRepositoryException(String message) {
        super(message);
    }

    public NoSuchRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
