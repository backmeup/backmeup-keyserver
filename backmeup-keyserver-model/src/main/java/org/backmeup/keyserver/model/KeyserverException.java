package org.backmeup.keyserver.model;

public class KeyserverException extends Exception {

    private static final long serialVersionUID = 3673890017686453729L;
    private final boolean causedByCryptoException;

    public KeyserverException(String message) {
        super(message);
        this.causedByCryptoException = false;
    }

    public KeyserverException(Throwable cause) {
        super(cause);
        this.causedByCryptoException = this.getCause() instanceof CryptoException;
    }

    public KeyserverException(String message, Throwable cause) {
        super(message, cause);
        this.causedByCryptoException = this.getCause() instanceof CryptoException;
    }
    
    
    public boolean isCausedByCryptoException() {
        return this.causedByCryptoException;
    }
}
