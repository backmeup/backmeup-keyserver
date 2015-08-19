package org.backmeup.keyserver.crypto;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

public class Keyring {
    private int keyringId;
    private Map<String, byte[]> peppers;
    private String hashAlgorithm;
    private String keyStretchingAlgorithm;
    private String symmetricEncryptionAlgorithm;
    private int symmetricEncryptionKeyLength;
    private String asymmetricEncryptionAlgorithm;
    private int asymmetricEncryptionKeyLength;
    private String passwordAlgorithm;
    private int passwordLength;

    // TODO: Gültigkeitszeitraum notwendig?

    @SuppressWarnings("unused")
    private Keyring() {
    }

    public Keyring(int keyringId, Map<String, byte[]> peppers, String hashAlgorithm, String keyStretchingAlgorithm, String symmetricEncryptionAlgorithm,
            int symmetricEncryptionKeyLength, String asymmetricEncryptionAlgorithm, int asymmetricEncryptionKeyLength, String passwordAlgorithm, int passwordLength) {
        this.keyringId = keyringId;
        this.peppers = new HashMap<>();
        this.peppers.putAll(peppers);
        this.hashAlgorithm = hashAlgorithm;
        this.keyStretchingAlgorithm = keyStretchingAlgorithm;
        this.symmetricEncryptionAlgorithm = symmetricEncryptionAlgorithm;
        this.symmetricEncryptionKeyLength = symmetricEncryptionKeyLength;
        this.asymmetricEncryptionAlgorithm = asymmetricEncryptionAlgorithm;
        this.asymmetricEncryptionKeyLength = asymmetricEncryptionKeyLength;
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

    public String getSymmetricEncryptionAlgorithm() {
        return symmetricEncryptionAlgorithm;
    }

    public int getSymmetricEncryptionKeyLength() {
        return this.symmetricEncryptionKeyLength;
    }
    
    public String getAsymmetricEncryptionAlgorithm() {
        return asymmetricEncryptionAlgorithm;
    }

    public int getAsymmetricEncryptionKeyLength() {
        return this.asymmetricEncryptionKeyLength;
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
