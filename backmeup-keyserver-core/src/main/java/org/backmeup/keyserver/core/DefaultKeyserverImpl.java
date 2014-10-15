package org.backmeup.keyserver.core;

import static org.backmeup.keyserver.core.KeyserverUtils.concat;
import static org.backmeup.keyserver.core.KeyserverUtils.fromBase64String;
import static org.backmeup.keyserver.core.KeyserverUtils.toBase64String;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.codec.binary.StringUtils;
import org.backmeup.keyserver.core.crypto.CryptoException;
import org.backmeup.keyserver.core.crypto.EncryptionProvider;
import org.backmeup.keyserver.core.crypto.HashProvider;
import org.backmeup.keyserver.core.crypto.KeyStretchingProvider;
import org.backmeup.keyserver.core.crypto.Keyring;
import org.backmeup.keyserver.core.crypto.ProviderRegistry;
import org.backmeup.keyserver.core.db.Database;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.model.KeyserverEntry;

public class DefaultKeyserverImpl {	
	protected SortedMap<Integer, Keyring> keyrings = new TreeMap<>(Collections.reverseOrder());
	protected Keyring activeKeyring;
	private SecureRandom random = new SecureRandom();
	private Database db;
	
	public DefaultKeyserverImpl() {
		this.loadConfigFile();
		
		this.db = new org.backmeup.keyserver.core.db.derby.DatabaseImpl();
		try {
			this.db.connect();
		} catch (DatabaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void loadConfigFile() {
		//TODO: load keyring config file and initialize keyrings
		//simple fake it for now
		Map<String, byte[]> peppers = new HashMap<>();
		peppers.put("UserId", fromBase64String("5MlQkEfznxZSadtncDwqKVGfTGrcZ020pWrZJ5+WR3E="));
		peppers.put("ServiceUserId", fromBase64String("T0O0lfI0teC2cLdw+bxoubgPiu5HtUZkdxY5lbK1arc="));
		peppers.put("UserName", fromBase64String("7Z+P9DEhLl2fP0zgaIgqF6SRiOdfqHLXAP9Z4+Ff1OE="));
		
		Keyring k = new Keyring(1, peppers, "SHA-256", "SCRYPT", "AES/CBC/PKCS5Padding");
		
		this.keyrings.put(1, k);
		//TODO: set highest one as active keyring
		this.activeKeyring = k;
	}
	
	private byte[] getPepper(String application) {
		return this.activeKeyring.getPepper(application);
	}

	protected String hashStringWithPepper(String hashInput, String pepperApplication) throws CryptoException, NoSuchAlgorithmException {
		return hashStringWithPepper(this.activeKeyring, hashInput, pepperApplication);
	}
	
	protected String hashStringWithPepper(Keyring k, String hashInput, String pepperApplication) throws CryptoException, NoSuchAlgorithmException {
		return toBase64String(this.hashByteArrayWithPepper(k, StringUtils.getBytesUtf8(hashInput), pepperApplication));
	}
	
	protected byte[] hashByteArrayWithPepper(byte[] hashInput, String pepperApplication) throws CryptoException, NoSuchAlgorithmException {
		return this.hashByteArrayWithPepper(this.activeKeyring, hashInput, pepperApplication);
	}
	
	protected byte[] hashByteArrayWithPepper(Keyring k, byte[] hashInput, String pepperApplication) throws CryptoException, NoSuchAlgorithmException {
		HashProvider hp = ProviderRegistry.getHashProvider(k.getHashAlgorithm());
		return hp.hash(concat(hashInput, k.getPepper(pepperApplication)));
	}
	
	protected byte[] stretchStringWithPepper(String key, String pepperApplication) throws NoSuchAlgorithmException, CryptoException {
		return this.stretchStringWithPepper(this.activeKeyring, key, pepperApplication);
	}
	
	protected byte[] stretchStringWithPepper(Keyring k, String key, String pepperApplication) throws NoSuchAlgorithmException, CryptoException {
		KeyStretchingProvider kp = ProviderRegistry.getKeyStretchingProvider(k.getKeyStretchingAlgorithm());
		return kp.stretch(StringUtils.getBytesUtf8(key), k.getPepper(pepperApplication));
	}
	
	protected byte[] encryptString(byte[] key, String message) throws NoSuchAlgorithmException, CryptoException {
		return this.encryptString(this.activeKeyring, key, message);
	}
	
	protected byte[] encryptString(Keyring k, byte[] key, String message) throws CryptoException, NoSuchAlgorithmException {
		EncryptionProvider ep = ProviderRegistry.getEncryptionProvider(k.getEncryptionAlgorithm());
		return ep.encrypt(key, StringUtils.getBytesUtf8(message));
	}
	
	protected String decryptString(byte[] key, byte[] message) throws NoSuchAlgorithmException, CryptoException {
		return this.decryptString(this.activeKeyring, key, message);
	}
	
	protected String decryptString(Keyring k, byte[] key, byte[] message) throws CryptoException, NoSuchAlgorithmException {
		EncryptionProvider ep = ProviderRegistry.getEncryptionProvider(k.getEncryptionAlgorithm());
		return StringUtils.newStringUtf8(ep.decrypt(key, message));
	}
	
	protected KeyserverEntry searchForEntry(String[] hashInputs, String[] pepperApplications, String keyPattern) throws NoSuchAlgorithmException, CryptoException, DatabaseException {
		MessageFormat key = new MessageFormat(keyPattern);
		
		for (Keyring k : this.keyrings.values()) {
			String[] hashes = new String[hashInputs.length];
			for(int i=0; i<hashInputs.length; i++) {
				hashes[i] = this.hashStringWithPepper(k, hashInputs[i], pepperApplications[i]);
			}
			
			//System.out.println(key.format(hashes));
			KeyserverEntry entry = this.db.getEntry(key.format(hashes));
			if (entry != null)
				return entry;
		}
		
		return null;
	}
	
	protected KeyserverEntry searchForEntry(String hashInput, String pepperApplication, String keyPattern) throws NoSuchAlgorithmException, CryptoException, DatabaseException {
		return this.searchForEntry(new String[]{hashInput}, new String[]{pepperApplication}, keyPattern);
	}

	public String registerUser(String username, String password) throws KeyserverException {
		String userId = null;
		String serviceUserId = null;
		
		try {
			KeyserverEntry alreadyExistingUser = this.searchForEntry(username, "UserName", "{0}.UserName");
			if (alreadyExistingUser != null) {
				throw new KeyserverException("duplicate username");
			}
			
			boolean collission = false;
			do {
				byte[] userKey = new byte[32];
				random.nextBytes(userKey);
				
				userId = toBase64String(this.hashByteArrayWithPepper(userKey, "UserId"));
				serviceUserId = toBase64String(this.hashByteArrayWithPepper(userKey, "ServiceUserId"));
				
				KeyserverEntry uid = this.db.getEntry(userId+".UserId");
				KeyserverEntry suid = this.db.getEntry(serviceUserId+".ServiceUserId");
				collission = (uid != null) || (suid != null);
			}
			while(collission);
			
			String usernameHash = this.hashStringWithPepper(username, "UserName");
			/*System.out.println(userId);
			System.out.println(serviceUserId);
			System.out.println(usernameHash);*/
			
			KeyserverEntry ke = new KeyserverEntry(userId+".UserId");
			db.putEntry(ke);
			
			ke = new KeyserverEntry(serviceUserId+".ServiceUserId");
			db.putEntry(ke);
			
			byte[] key = this.stretchStringWithPepper(username, "UserName");
			byte[] payload = this.encryptString(this.hashByteArrayWithPepper(key, "UserName"), userId);
			
			ke = new KeyserverEntry(usernameHash+".UserName");
			ke.setValue(payload);
			db.putEntry(ke);
		} catch(CryptoException | DatabaseException | NoSuchAlgorithmException e) {
			throw new KeyserverException(e);
		}
		
		return serviceUserId;
	}
	
	protected String getUserId(String username) throws KeyserverException {
		try {
			KeyserverEntry usernameEntry = this.searchForEntry(username, "UserName", "{0}.UserName");
			if (usernameEntry == null) {
				throw new KeyserverException("username not found");
			}
			
			if (usernameEntry.getKeyringId() < this.activeKeyring.getKeyringId()) {
				//TODO: migrate Entry
			}
			
			byte[] key = this.stretchStringWithPepper(username, "UserName");
			return this.decryptString(this.hashByteArrayWithPepper(key, "UserName"), usernameEntry.getValue());
		} catch(DatabaseException | CryptoException | NoSuchAlgorithmException e) {
			throw new KeyserverException(e);
		}
	}
}
