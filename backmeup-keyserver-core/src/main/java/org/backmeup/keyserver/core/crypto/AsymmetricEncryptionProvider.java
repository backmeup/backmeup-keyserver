package org.backmeup.keyserver.core.crypto;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.backmeup.keyserver.model.CryptoException;

public interface AsymmetricEncryptionProvider {
    String getAlgorithm();

    KeyPair generateKey(int length) throws CryptoException;

    byte[] encrypt(PublicKey key, byte[] message) throws CryptoException;

    byte[] decrypt(PrivateKey key, byte[] enrcypted) throws CryptoException;
}