package org.backmeup.keyserver.crypto;

import org.backmeup.keyserver.model.CryptoException;

public interface SymmetricEncryptionProvider {
    String getAlgorithm();

    byte[] generateKey(int length) throws CryptoException;

    byte[] encrypt(byte[] key, byte[] message) throws CryptoException;

    byte[] decrypt(byte[] key, byte[] enrcypted) throws CryptoException;
}