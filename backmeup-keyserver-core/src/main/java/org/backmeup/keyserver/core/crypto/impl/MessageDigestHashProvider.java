package org.backmeup.keyserver.core.crypto.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.backmeup.keyserver.core.crypto.CryptoException;
import org.backmeup.keyserver.core.crypto.HashProvider;

public class MessageDigestHashProvider implements HashProvider {	
	private String algorithm;
	private MessageDigest digest;
	
	@Override
	public String getAlgorithm() {
		return this.algorithm;	
	}
	
	public MessageDigestHashProvider(String algorithm) {
		this.algorithm = algorithm;
		try {
			this.digest = MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	@Override
	public byte[] hash(byte[] message)  throws CryptoException {
		digest.reset();
		return digest.digest(message);
	}
}
