package org.backmeup.keyserver.crypto.impl;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.backmeup.keyserver.crypto.EncryptionUtils;
import org.backmeup.keyserver.crypto.SymmetricEncryptionProvider;
import org.backmeup.keyserver.model.CryptoException;

public class AESEncryptionProvider implements SymmetricEncryptionProvider {
    public static final int IV_LENGTH = 16;

    private String algorithm;
    private Cipher cipher;
    private KeyGenerator keyGen;

    public AESEncryptionProvider(String algorithm) {
        this.algorithm = algorithm;
        try {
            this.cipher = Cipher.getInstance(algorithm);
            this.keyGen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String getAlgorithm() {
        return this.algorithm;
    }

    @Override
    public Cipher getCipher() {
        return this.cipher;
    }
    
    @Override
    public synchronized byte[] generateKey(int length) {
        this.keyGen.init(length);
        return keyGen.generateKey().getEncoded();
    }
    
    @Override
    public byte[] getIV() throws CryptoException {
        AlgorithmParameters params = cipher.getParameters();
        try {
            return params.getParameterSpec(IvParameterSpec.class).getIV();
        } catch (InvalidParameterSpecException e) {
            throw new CryptoException(e);
        }
    }
    
    @Override
    public synchronized byte[] encrypt(byte[] key, byte[] message) throws CryptoException {
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        try {
            this.cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] ivBytes = this.getIV();
            return EncryptionUtils.concat(ivBytes, cipher.doFinal(message));
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public synchronized byte[] decrypt(byte[] key, byte[] encrypted) throws CryptoException {
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        byte[][] ivAndEncrypted = EncryptionUtils.split(encrypted, IV_LENGTH);
        try {
            this.cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(ivAndEncrypted[0]));
            return cipher.doFinal(ivAndEncrypted[1]);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
            throw new CryptoException(e);
        }
    }
}
