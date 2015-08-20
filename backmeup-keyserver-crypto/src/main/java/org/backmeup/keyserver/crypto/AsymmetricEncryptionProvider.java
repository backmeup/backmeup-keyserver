package org.backmeup.keyserver.crypto;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;

import org.backmeup.keyserver.model.CryptoException;

public interface AsymmetricEncryptionProvider {
    String getAlgorithm();
    
    Cipher getCipher();

    KeyPair generateKey(int length) throws CryptoException;

    byte[] encrypt(PublicKey key, byte[] message) throws CryptoException;

    byte[] decrypt(PrivateKey key, byte[] enrcypted) throws CryptoException;
}