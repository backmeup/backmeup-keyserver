package org.backmeup.keyserver.core.crypto;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.backmeup.keyserver.core.crypto.impl.AESEncryptionProvider;
import org.backmeup.keyserver.core.crypto.impl.MessageDigestHashProvider;
import org.backmeup.keyserver.core.crypto.impl.SCryptKeyStretchingProvider;

public class ProviderRegistry {
	private static Map<String, HashProvider> hashProviders;
	private static Map<String, KeyStretchingProvider> keyStretchingProviders;
	private static Map<String, EncryptionProvider> encryptionProviders;
	
	static {
		hashProviders = new HashMap<>();
		keyStretchingProviders = new HashMap<>();
		encryptionProviders = new HashMap<>();
	}
	
	static {
		//load default providers
		//TODO: how to handle dynamically/by configuration
		registerHashProvider("SHA-256", new MessageDigestHashProvider("SHA-256"));
		registerHashProvider("SHA-512", new MessageDigestHashProvider("SHA-512"));
		
		registerEncryptionProvider("AES/CBC/PKCS5Padding", new AESEncryptionProvider("AES/CBC/PKCS5Padding"));
		
		registerKeyStretchingProvider("SCRYPT", new SCryptKeyStretchingProvider());
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
	
	public static HashProvider getHashProvider(String algorithm) throws NoSuchAlgorithmException {
		HashProvider provider = hashProviders.get(algorithm);
		if (provider == null)
			throw new NoSuchAlgorithmException("No hash provider for algorithm "+algorithm);
		return provider;
	}
	
	public static KeyStretchingProvider getKeyStretchingProvider(String algorithm) throws NoSuchAlgorithmException {
		KeyStretchingProvider provider = keyStretchingProviders.get(algorithm);
		if (provider == null)
			throw new NoSuchAlgorithmException("No key stretching provider for algorithm "+algorithm);
		return provider;
	}
	
	public static EncryptionProvider getEncryptionProvider(String algorithm) throws NoSuchAlgorithmException {
		EncryptionProvider provider =  encryptionProviders.get(algorithm);
		if (provider == null)
			throw new NoSuchAlgorithmException("No encryption provider for algorithm "+algorithm);
		return provider;
	}
}
