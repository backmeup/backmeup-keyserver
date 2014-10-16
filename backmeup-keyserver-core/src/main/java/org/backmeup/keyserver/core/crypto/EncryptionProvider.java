package org.backmeup.keyserver.core.crypto;

public interface EncryptionProvider {
	public String getAlgorithm();
	public byte[] generateKey(int length) throws CryptoException;
	public byte[] encrypt(byte[] key, byte[] message)  throws CryptoException;
	public byte[] decrypt(byte[] key, byte[] enrcypted)  throws CryptoException;
}