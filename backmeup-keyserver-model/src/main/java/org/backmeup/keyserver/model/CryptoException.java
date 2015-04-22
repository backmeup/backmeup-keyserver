package org.backmeup.keyserver.model;

public class CryptoException extends Exception {

    private static final long serialVersionUID = -1156528562835655354L;

    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(Throwable cause) {
        super(cause);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
