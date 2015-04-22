package org.backmeup.keyserver.core.crypto;

import org.backmeup.keyserver.model.CryptoException;

public interface HashProvider {
    String getAlgorithm();

    byte[] hash(byte[] message) throws CryptoException;
}
