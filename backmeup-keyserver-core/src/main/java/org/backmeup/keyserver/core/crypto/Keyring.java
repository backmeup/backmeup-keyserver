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
    private String passwordAlgorithm;
    private int passwordLength;

    // TODO: Gültigkeitszeitraum notwendig?

    @SuppressWarnings("unused")
    private Keyring() {
    }

    public Keyring(int keyringId, Map<String, byte[]> peppers, String hashAlgorithm, String keyStretchingAlgorithm, String encryptionAlgorithm,
            int encryptionKeyLength, String passwordAlgorithm, int passwordLength) {
        this.keyringId = keyringId;
        this.peppers = new HashMap<>();
        this.peppers.putAll(peppers);
        this.hashAlgorithm = hashAlgorithm;
        this.keyStretchingAlgorithm = keyStretchingAlgorithm;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.encryptionKeyLength = encryptionKeyLength;
        this.passwordAlgorithm = passwordAlgorithm;
        this.passwordLength = passwordLength;
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

    public int getEncryptionKeyLength() {
        return this.encryptionKeyLength;
    }

    public String getPasswordAlgorithm() {
        return passwordAlgorithm;
    }

    public int getPasswordLength() {
        return this.passwordLength;
    }

    //needed for jackson mapping
    @SuppressWarnings("all") 
    private void setPeppers(final Map<String, byte[]> peppers) {
           this.peppers = peppers;
    }

    public byte[] getPepper(String application) {
        if (this.peppers.containsKey(application)) {
            return this.peppers.get(application);
        } else {
            throw new MissingResourceException("pepper " + application + " not found in keyring " + this.keyringId, "keyring " + this.keyringId,
                    application);
        }
    }
}
