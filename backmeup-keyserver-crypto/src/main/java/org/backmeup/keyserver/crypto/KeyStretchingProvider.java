package org.backmeup.keyserver.crypto;

import org.backmeup.keyserver.model.CryptoException;

public interface KeyStretchingProvider {
    String getAlgorithm();

    byte[] stretch(byte[] key, byte[] salt) throws CryptoException;
}
