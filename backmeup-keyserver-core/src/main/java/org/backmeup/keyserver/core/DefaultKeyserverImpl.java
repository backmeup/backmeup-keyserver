package org.backmeup.keyserver.core;

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

import org.backmeup.keyserver.core.config.Configuration;
import org.backmeup.keyserver.core.crypto.CryptoException;
import org.backmeup.keyserver.core.crypto.Keyring;
import org.backmeup.keyserver.core.db.Database;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.model.AppUser;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.KeyserverEntry;
import org.codehaus.jackson.map.ObjectMapper;

public class DefaultKeyserverImpl implements Keyserver {	
	protected SortedMap<Integer, Keyring> keyrings = new TreeMap<>(Collections.reverseOrder());
	protected Keyring activeKeyring;
	protected SecureRandom random = new SecureRandom();
	protected Database db;
	protected ObjectMapper jsonMapper = new ObjectMapper();
	
	protected String serviceId = null;
	protected String servicePassword = null;
	protected int uiTokenTimeout;
	
	protected DefaultAppLogic appLogic;
	protected DefaultUserLogic userLogic;
	protected DefaultTokenLogic tokenLogic;
	
	public DefaultKeyserverImpl() {
		this.loadKeyserverConfigFile();
		
		this.serviceId = Configuration.getProperty("backmeup.service.id");
		this.servicePassword = Configuration.getProperty("backmeup.service.password");
		this.uiTokenTimeout = Integer.parseInt(Configuration.getProperty("backmeup.keyserver.uiTokenTimeout"));
		
		this.db = new org.backmeup.keyserver.core.db.derby.DatabaseImpl();
		try {
			this.db.connect();
		} catch (DatabaseException e) {
			throw new Error("keyserver cannot connect to database", e);
		}
		
		this.appLogic = new DefaultAppLogic(this);
		this.userLogic = new DefaultUserLogic(this);
		this.tokenLogic = new DefaultTokenLogic(this);
	}
	
	private void loadKeyserverConfigFile() {
		//TODO: load keyring config file and initialize keyrings
		//simple fake it for now
		Map<String, byte[]> peppers = new HashMap<>();
		peppers.put("UserId", fromBase64String("5MlQkEfznxZSadtncDwqKVGfTGrcZ020pWrZJ5+WR3E="));
		peppers.put("ServiceUserId", fromBase64String("T0O0lfI0teC2cLdw+bxoubgPiu5HtUZkdxY5lbK1arc="));
		peppers.put("UserName", fromBase64String("7Z+P9DEhLl2fP0zgaIgqF6SRiOdfqHLXAP9Z4+Ff1OE="));
		peppers.put("Account", fromBase64String("Y3WIQAJGXFteocB3j4+wHfsvYoTcH19kvcBgCMl7vKI="));
		peppers.put("App", fromBase64String("OEv+feVGv/qLYPYtgE9LNWtuEZ93km3l5iNTVy24L6Q="));
		peppers.put("InternalToken", fromBase64String("8hnYznxAPvD1M2+675voGToc1J08DimzWcgoGcWupeI="));
		
		Keyring k = new Keyring(1, peppers, "SHA-256", "SCRYPT", "AES/CBC/PKCS5Padding", 256);
		this.keyrings.put(1, k);
		
		//set highest one as active keyring
		this.activeKeyring = this.keyrings.get(this.keyrings.firstKey());
	}
	
	protected KeyserverEntry searchForEntry(String[] hashInputs, String[] pepperApplications, String keyPattern) throws NoSuchAlgorithmException, CryptoException, DatabaseException {
		MessageFormat key = new MessageFormat(keyPattern);
		
		for (Keyring k : this.keyrings.values()) {
			String[] hashes = new String[hashInputs.length];
			for(int i=0; i<hashInputs.length; i++) {
				hashes[i] = KeyserverUtils.hashStringWithPepper(k, hashInputs[i], pepperApplications[i]);
			}
			
			KeyserverEntry entry = this.db.getEntry(key.format(hashes));
			if (entry != null)
				return entry;
		}
		
		return null;
	}
	
	protected KeyserverEntry searchForEntry(String hashInput, String pepperApplication, String keyPattern) throws NoSuchAlgorithmException, CryptoException, DatabaseException {
		return this.searchForEntry(new String[]{hashInput}, new String[]{pepperApplication}, keyPattern);
	}
	
	protected KeyserverEntry searchForEntry(byte[][] hashInputs, String[] pepperApplications, String keyPattern) throws NoSuchAlgorithmException, CryptoException, DatabaseException {
		MessageFormat key = new MessageFormat(keyPattern);
		
		for (Keyring k : this.keyrings.values()) {
			String[] hashes = new String[hashInputs.length];
			for(int i=0; i<hashInputs.length; i++) {
				hashes[i] = toBase64String(KeyserverUtils.hashByteArrayWithPepper(k, hashInputs[i], pepperApplications[i]));
			}
			
			KeyserverEntry entry = this.db.getEntry(key.format(hashes));
			if (entry != null)
				return entry;
		}
		
		return null;
	}
	
	protected KeyserverEntry searchForEntry(byte[] hashInput, String pepperApplication, String keyPattern) throws NoSuchAlgorithmException, CryptoException, DatabaseException {
		return this.searchForEntry(new byte[][]{hashInput}, new String[]{pepperApplication}, keyPattern);
	}
	
	@Override
	public String registerUser(String username, String password) throws KeyserverException {
		return this.userLogic.registerUser(username, password);
	}
	
	@Override
	public String registerAnonoumysUser(String username, String password) throws KeyserverException {
		return this.userLogic.registerAnonoumysUser(username, password);
	}
	
	@Override
	public AuthResponse authenticateUserWithPassword(String username, String password) throws KeyserverException {
		return this.userLogic.authenticateWithPassword(username, password);
	}
	
	@Override
	public AppUser registerApp(AppUser.Approle role) throws KeyserverException {
		return this.appLogic.register(role);
	}
	
	@Override
	public void removeApp(String appId) throws KeyserverException {
		this.appLogic.remove(appId);
	}
	
	@Override
	public AppUser authenticateApp(String appId, String appKey) throws KeyserverException {
		return this.appLogic.authenticate(appId, appKey);
	}
}
