package org.backmeup.keyserver.core.crypto;

public interface HashProvider {
	public String getAlgorithm();
	public byte[] hash(byte[] message) throws CryptoException;
}
