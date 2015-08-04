package org.backmeup.keyserver.core.crypto.impl;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import org.backmeup.keyserver.core.crypto.AsymmetricEncryptionProvider;
import org.backmeup.keyserver.model.CryptoException;

public class RSAEncryptionProvider implements AsymmetricEncryptionProvider {

    private String algorithm;
    private Cipher cipher;
    private KeyPairGenerator keyGen;

    public RSAEncryptionProvider() {
        this.algorithm = "RSA";
        try {
            this.cipher = Cipher.getInstance(this.algorithm);
            this.keyGen = KeyPairGenerator.getInstance(this.algorithm);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String getAlgorithm() {
        return this.algorithm;
    }

    @Override
    public synchronized KeyPair generateKey(int length) throws CryptoException {
        keyGen.initialize(length);
        return keyGen.genKeyPair();
    }

    @Override
    public synchronized byte[] encrypt(PublicKey key, byte[] message) throws CryptoException {
        try {
            this.cipher.init(Cipher.ENCRYPT_MODE, key);
            return this.cipher.doFinal(message);
        } catch (Exception e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public synchronized byte[] decrypt(PrivateKey key, byte[] encrypted) throws CryptoException {
        try {
            this.cipher.init(Cipher.DECRYPT_MODE, key);
            return this.cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new CryptoException(e);
        }
    }
}
