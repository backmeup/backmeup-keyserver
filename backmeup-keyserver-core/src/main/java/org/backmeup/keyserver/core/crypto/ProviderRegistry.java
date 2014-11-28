package org.backmeup.keyserver.core.crypto;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.backmeup.keyserver.core.crypto.impl.AESEncryptionProvider;
import org.backmeup.keyserver.core.crypto.impl.AsciiPasswordProvider;
import org.backmeup.keyserver.core.crypto.impl.MessageDigestHashProvider;
import org.backmeup.keyserver.core.crypto.impl.SCryptKeyStretchingProvider;

public class ProviderRegistry {
    private static Map<String, HashProvider> hashProviders = new HashMap<>();
    private static Map<String, KeyStretchingProvider> keyStretchingProviders = new HashMap<>();
    private static Map<String, EncryptionProvider> encryptionProviders = new HashMap<>();
    private static Map<String, PasswordProvider> passwordProviders = new HashMap<>();

    static {
        loadProviders();
    }

    private ProviderRegistry() {

    }

    @SuppressWarnings("all")
    private static void loadProviders() {
        // load default providers
        // TODO: how to handle dynamically/by configuration
        registerHashProvider("SHA-256", new MessageDigestHashProvider("SHA-256"));
        registerHashProvider("SHA-512", new MessageDigestHashProvider("SHA-512"));
        registerEncryptionProvider("AES/CBC/PKCS5Padding", new AESEncryptionProvider("AES/CBC/PKCS5Padding"));
        registerKeyStretchingProvider("SCRYPT", new SCryptKeyStretchingProvider());
        registerPasswordProvider("ASCII", new AsciiPasswordProvider());
    }

    public static void registerHashProvider(String algorithm, HashProvider provider) {
        hashProviders.put(algorithm, provider);
    }

    public static void registerKeyStretchingProvider(String algorithm, KeyStretchingProvider provider) {
        keyStretchingProviders.put(algorithm, provider);
    }

    public static void registerEncryptionProvider(String algorithm, EncryptionProvider provider) {
        encryptionProviders.put(algorithm, provider);
    }

    public static void registerPasswordProvider(String algorithm, PasswordProvider provider) {
        passwordProviders.put(algorithm, provider);
    }

    private static <T> T getProvider(Map<String, T> providerList, String algorithm) throws NoSuchAlgorithmException {
        T provider = providerList.get(algorithm);
        if (provider == null) {
            throw new NoSuchAlgorithmException("No provider for algorithm " + algorithm);
        }
        return provider;
    }

    public static HashProvider getHashProvider(String algorithm) throws NoSuchAlgorithmException {
        return getProvider(hashProviders, algorithm);
    }

    public static KeyStretchingProvider getKeyStretchingProvider(String algorithm) throws NoSuchAlgorithmException {
        return getProvider(keyStretchingProviders, algorithm);
    }

    public static EncryptionProvider getEncryptionProvider(String algorithm) throws NoSuchAlgorithmException {
        return getProvider(encryptionProviders, algorithm);
    }

    public static PasswordProvider getPasswordProvider(String algorithm) throws NoSuchAlgorithmException {
        return getProvider(passwordProviders, algorithm);
    }
}
