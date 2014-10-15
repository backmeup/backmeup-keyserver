package org.backmeup.keyserver.core.crypto;

public interface KeyStretchingProvider {
	public String getAlgorithm();
	public byte[] stretch(byte[] key, byte[] salt)  throws CryptoException;
}
