package org.backmeup.keyserver.core;

import static org.backmeup.keyserver.core.KeyserverUtils.*;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.backmeup.keyserver.core.crypto.CryptoException;
import org.backmeup.keyserver.core.crypto.Keyring;
import org.backmeup.keyserver.core.db.Database;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.model.KeyserverEntry;
import org.codehaus.jackson.node.ObjectNode;

public class DefaultUserLogic {
	private DefaultKeyserverImpl keyserver;
	private Keyring keyring;
	private Database db;
	
	public DefaultUserLogic(DefaultKeyserverImpl keyserver) {
		this.keyserver = keyserver;
		this.keyring = this.keyserver.activeKeyring;
		this.db = this.keyserver.db;
	}

	protected Map<String, String> createBaseUser(String username, String password) throws KeyserverException {
		String userId = null;
		String serviceUserId = null;
		
		try {
			KeyserverEntry alreadyExistingUser = this.keyserver.searchForEntry(username, "UserName", "{0}.UserName");
			if (alreadyExistingUser != null) {
				throw new KeyserverException("duplicate username");
			}
			
			boolean collission = false;
			do {
				byte[] userKey = new byte[32];
				this.keyserver.random.nextBytes(userKey);
				
				userId = toBase64String(hashByteArrayWithPepper(this.keyring, userKey, "UserId"));
				serviceUserId = toBase64String(hashByteArrayWithPepper(this.keyring, userKey, "ServiceUserId"));
				
				KeyserverEntry uid = this.db.getEntry(userId+".UserId");
				KeyserverEntry suid = this.db.getEntry(serviceUserId+".ServiceUserId");
				collission = (uid != null) || (suid != null);
			}
			while(collission);
			
			//[UserId].UserId
			KeyserverEntry ke = new KeyserverEntry(userId+".UserId");
			db.putEntry(ke);
			
			//[ServiceUserId].ServiceUserId
			ke = new KeyserverEntry(serviceUserId+".ServiceUserId");
			db.putEntry(ke);
		} catch(CryptoException | DatabaseException | NoSuchAlgorithmException e) {
			throw new KeyserverException(e);
		}
		
		Map<String, String> ret = new HashMap<>();
		ret.put("userId", userId);
		ret.put("serviceUserId", serviceUserId);
		return ret;
	}

	public String registerUser(String username, String password) throws KeyserverException {
		String userId = null;
		String serviceUserId = null;
		
		try {
			Map<String, String> ids = this.createBaseUser(username, password);
			userId = ids.get("userId");
			serviceUserId = ids.get("serviceUserId");
			
			//[Hash(Benutzername)].UserName
			String usernameHash = hashStringWithPepper(this.keyring, username, "UserName");
			
			byte[] key = stretchStringWithPepper(this.keyring, username, "UserName");
			byte[] payload = encryptString(this.keyring, hashByteArrayWithPepper(this.keyring, key, "UserName"), userId);
			
			KeyserverEntry ke = new KeyserverEntry(usernameHash+".UserName");
			ke.setValue(payload);
			db.putEntry(ke);
			
			//[UserId].Account
			byte[] accountKey = generateKey(this.keyring);
			key = stretchStringWithPepper(this.keyring, username+";"+password, "Account");
			
			ObjectNode valueNode = this.keyserver.jsonMapper.createObjectNode();
			valueNode.put("ServiceUserId", serviceUserId);
			valueNode.put("AccountKey", accountKey);
			payload = encryptString(this.keyring, hashByteArrayWithPepper(this.keyring, key, "Account"), valueNode.toString());
			
			ke = new KeyserverEntry(userId+".Account");
			ke.setValue(payload);
			db.putEntry(ke);
		} catch(CryptoException | DatabaseException | NoSuchAlgorithmException e) {
			throw new KeyserverException(e);
		}
		
		return serviceUserId;
	}
	
	public String registerAnonoumysUser(String username, String password) throws KeyserverException {
		String userId = null;
		String serviceUserId = null;
		
		//try {
			Map<String, String> ids = this.createBaseUser(username, password);
			userId = ids.get("userId");
			serviceUserId = ids.get("serviceUserId");
		/*} catch(CryptoException | DatabaseException | NoSuchAlgorithmException e) {
			throw new KeyserverException(e);
		}*/
		
		return serviceUserId;
	}
	
	protected String getUserId(String username) throws KeyserverException {
		try {
			KeyserverEntry usernameEntry = this.keyserver.searchForEntry(username, "UserName", "{0}.UserName");
			if (usernameEntry == null) {
				throw new KeyserverException("username not found");
			}
			
			if (usernameEntry.getKeyringId() < this.keyring.getKeyringId()) {
				//TODO: migrate Entry
			}
			
			byte[] key = stretchStringWithPepper(this.keyring, username, "UserName");
			return decryptString(this.keyring, hashByteArrayWithPepper(this.keyring, key, "UserName"), usernameEntry.getValue());
		} catch(DatabaseException | CryptoException | NoSuchAlgorithmException e) {
			throw new KeyserverException(e);
		}
	}
}
