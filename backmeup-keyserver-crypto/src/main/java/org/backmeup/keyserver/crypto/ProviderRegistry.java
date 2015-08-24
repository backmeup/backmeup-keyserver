package org.backmeup.keyserver.crypto;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.backmeup.keyserver.crypto.impl.AESEncryptionProvider;
import org.backmeup.keyserver.crypto.impl.AsciiPasswordProvider;
import org.backmeup.keyserver.crypto.impl.MessageDigestHashProvider;
import org.backmeup.keyserver.crypto.impl.RSAEncryptionProvider;
import org.backmeup.keyserver.crypto.impl.SCryptKeyStretchingProvider;

public class ProviderRegistry {
    private static Map<String, HashProvider> hashProviders = new HashMap<>();
    private static Map<String, KeyStretchingProvider> keyStretchingProviders = new HashMap<>();
    private static Map<String, SymmetricEncryptionProvider> symmetricEncryptionProviders = new HashMap<>();
    private static Map<String, AsymmetricEncryptionProvider> asymmetricEncryptionProviders = new HashMap<>();
    private static Map<String, PasswordProvider> passwordProviders = new HashMap<>();

    private ProviderRegistry() {
    }

    public static void registerHashProvider(String algorithm, HashProvider provider) {
        hashProviders.put(algorithm, provider);
    }

    public static void registerKeyStretchingProvider(String algorithm, KeyStretchingProvider provider) {
        keyStretchingProviders.put(algorithm, provider);
    }

    public static void registerSymmetricEncryptionProvider(String algorithm, SymmetricEncryptionProvider provider) {
        symmetricEncryptionProviders.put(algorithm, provider);
    }
    
    public static void registerAsymmetricEncryptionProvider(String algorithm, AsymmetricEncryptionProvider provider) {
        asymmetricEncryptionProviders.put(algorithm, provider);
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

    public static SymmetricEncryptionProvider getSymmetricEncryptionProvider(String algorithm) throws NoSuchAlgorithmException {
        return getProvider(symmetricEncryptionProviders, algorithm);
    }
    
    public static AsymmetricEncryptionProvider getAsymmetricEncryptionProvider(String algorithm) throws NoSuchAlgorithmException {
        return getProvider(asymmetricEncryptionProviders, algorithm);
    }

    public static PasswordProvider getPasswordProvider(String algorithm) throws NoSuchAlgorithmException {
        return getProvider(passwordProviders, algorithm);
    }
}
