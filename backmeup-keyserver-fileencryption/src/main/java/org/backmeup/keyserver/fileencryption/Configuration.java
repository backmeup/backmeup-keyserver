package org.backmeup.keyserver.fileencryption;

public class Configuration {
    public static final String AES_MODE = "AES/CBC/PKCS5Padding";
    public static final int AES_KEY_LENGTH = 256;
    public static final String KEYSTORE_SUFFIX = ".keys";
    
    
    private Configuration() {
    }

}
