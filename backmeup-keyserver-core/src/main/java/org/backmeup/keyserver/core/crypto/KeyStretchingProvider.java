package org.backmeup.keyserver.core.crypto;

public interface KeyStretchingProvider {
    String getAlgorithm();

    byte[] stretch(byte[] key, byte[] salt) throws CryptoException;
}
