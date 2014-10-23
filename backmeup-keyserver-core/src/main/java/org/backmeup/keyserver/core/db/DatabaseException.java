package org.backmeup.keyserver.core.db;

public class DatabaseException extends Exception {

    private static final long serialVersionUID = -8382351826921683525L;

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(Throwable cause) {
        super(cause);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
