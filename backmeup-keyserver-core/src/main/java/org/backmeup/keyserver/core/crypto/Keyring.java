package org.backmeup.keyserver.core.crypto;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

public class Keyring {
	private int keyringId;
	private Map<String, byte[]> peppers;
	private String hashAlgorithm;
	private String keyStretchingAlgorithm;
	private String encryptionAlgorithm;
	private int encryptionKeyLength;
	//TODO: Gültigkeitszeitraum notwendig?
	
	public Keyring(int keyringId, Map<String, byte[]> peppers, String hashAlgorithm, String keyStretchingAlgorithm, String encryptionAlgorithm, int encryptionKeyLength) {
		this.keyringId = keyringId;
		this.peppers = new HashMap<>();
		this.peppers.putAll(peppers);
		this.hashAlgorithm = hashAlgorithm;
		this.keyStretchingAlgorithm = keyStretchingAlgorithm;
		this.encryptionAlgorithm = encryptionAlgorithm;
		this.encryptionKeyLength = encryptionKeyLength;
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
		byte[] pepper = this.peppers.get(application);
		if (pepper == null) {
			throw new MissingResourceException("pepper "+application+" not found in keyring " + this.keyringId, "keyring " + this.keyringId, application);
		}
		
		return pepper;
	}
	
	public int getEncryptionKeyLength() {
		return this.encryptionKeyLength;
	}
}
