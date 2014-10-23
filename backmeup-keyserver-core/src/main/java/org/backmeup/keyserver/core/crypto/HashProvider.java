package org.backmeup.keyserver.core.crypto;

public interface HashProvider {
    String getAlgorithm();

    byte[] hash(byte[] message) throws CryptoException;
}
