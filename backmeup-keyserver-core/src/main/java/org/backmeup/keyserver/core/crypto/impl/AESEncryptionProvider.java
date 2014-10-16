package org.backmeup.keyserver.core.crypto.impl;

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
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.backmeup.keyserver.core.KeyserverUtils;
import org.backmeup.keyserver.core.crypto.CryptoException;
import org.backmeup.keyserver.core.crypto.EncryptionProvider;

public class AESEncryptionProvider implements EncryptionProvider {
	public static final int IV_LENGTH = 16;
	
	private String algorithm;
	private Cipher cipher;
	private KeyGenerator keyGen;

	
	@Override
	public String getAlgorithm() {
		return this.algorithm;	
	}
	
	public AESEncryptionProvider(String algorithm) {
		this.algorithm = algorithm;
		try {
			this.cipher = Cipher.getInstance(algorithm);
			this.keyGen = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public byte[] generateKey(int length) {
		this.keyGen.init(length);
		return keyGen.generateKey().getEncoded();
	}
	
	@Override
	public byte[] encrypt(byte[] key, byte[] message) throws CryptoException {
		SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
		try {
			this.cipher.init(Cipher.ENCRYPT_MODE, keySpec);
			AlgorithmParameters params = cipher.getParameters();
	        byte[] ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();
	        return KeyserverUtils.concat(ivBytes, cipher.doFinal(message));
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidParameterSpecException e) {
			throw new CryptoException(e);
		}
	}

	@Override
	public byte[] decrypt(byte[] key, byte[] enrcypted) throws CryptoException {
		SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
		byte[][] ivAndEncrypted = KeyserverUtils.split(enrcypted, IV_LENGTH);
		try {
			this.cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(ivAndEncrypted[0]));
			return cipher.doFinal(ivAndEncrypted[1]);
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
			throw new CryptoException(e);
		}
	}
}
