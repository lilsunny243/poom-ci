package org.codingmatters.poom.ci.dependency.flat;

public class GraphManagerException extends Exception {
    public GraphManagerException(String message) {
        super(message);
    }

    public GraphManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
