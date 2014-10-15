package org.backmeup.keyserver.core.crypto;

import java.util.HashMap;
import java.util.Map;

public class Keyring {
	private int keyringId;
	private Map<String, byte[]> peppers;
	private String hashAlgorithm;
	private String keyStretchingAlgorithm;
	private String encryptionAlgorithm;
	//TODO: Gültigkeitszeitraum notwendig?
	
	public Keyring(int keyringId, Map<String, byte[]> peppers, String hashAlgorithm, String keyStretchingAlgorithm, String encryptionAlgorithm) {
		this.keyringId = keyringId;
		this.peppers = new HashMap<>();
		this.peppers.putAll(peppers);
		this.hashAlgorithm = hashAlgorithm;
		this.keyStretchingAlgorithm = keyStretchingAlgorithm;
		this.encryptionAlgorithm = encryptionAlgorithm;
	}

	public int getKeyringId() {
		return keyringId;
	}

	public String getHashAlgorithm() {
		return hashAlgorithm;
	}

	public String getKeyStretchingAlgorithm() {
		return keyStretchingAlgorithm;
	}

	public String getEncryptionAlgorithm() {
		return encryptionAlgorithm;
	}
	
	public byte[] getPepper(String application) {
		return this.peppers.get(application);
	}
}
