package org.backmeup.keyserver.core.crypto;

public interface PasswordProvider {
    String getAlgorithm();

    String getPassword(int length) throws CryptoException;
}
