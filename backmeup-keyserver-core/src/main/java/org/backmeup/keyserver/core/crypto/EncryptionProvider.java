package org.backmeup.keyserver.core.crypto;

import org.backmeup.keyserver.model.CryptoException;

public interface EncryptionProvider {
    String getAlgorithm();

    byte[] generateKey(int length) throws CryptoException;

    byte[] encrypt(byte[] key, byte[] message) throws CryptoException;

    byte[] decrypt(byte[] key, byte[] enrcypted) throws CryptoException;
}