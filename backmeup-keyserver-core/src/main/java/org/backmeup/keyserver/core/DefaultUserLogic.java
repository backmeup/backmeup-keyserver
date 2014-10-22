package org.backmeup.keyserver.core;

import static org.backmeup.keyserver.core.KeyserverUtils.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.backmeup.keyserver.core.crypto.CryptoException;
import org.backmeup.keyserver.core.crypto.Keyring;
import org.backmeup.keyserver.core.db.Database;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.InternalTokenValue;
import org.backmeup.keyserver.model.KeyserverEntry;
import org.backmeup.keyserver.model.Token;
import org.backmeup.keyserver.model.TokenValue;
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
			valueNode.put("serviceUserId", serviceUserId);
			valueNode.put("accountKey", accountKey);
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
	
	public void remove(String username) throws KeyserverException {
		String userId = this.getUserId(username);
		//TODO
		/*
		try {
			KeyserverEntry appEntry = this.db.getEntry(appId+".App");
			if (appEntry == null) {
				throw new KeyserverException("appId not found");
			}
			
			appEntry.expire();
			this.db.updateTTL(appEntry);
		} catch(DatabaseException e) {
			throw new KeyserverException(e);
		}
		*/
	}
	
	public AuthResponse authenticateWithPassword(String username, String password) throws KeyserverException {
		String userId = this.getUserId(username);
		
		try {
			KeyserverEntry accountEntry = this.db.getEntry(userId+".Account");
			if (accountEntry == null) {
				throw new KeyserverException("account entry for "+ userId +" not found");
			}
			
			if (accountEntry.getKeyringId() < this.keyring.getKeyringId()) {
				//TODO: migrate Entry
			}
			
			byte[] key = stretchStringWithPepper(this.keyring, username+";"+password, "Account");
			String accountValue = decryptString(this.keyring, hashByteArrayWithPepper(this.keyring, key, "Account"), accountEntry.getValue());
			ObjectNode accountObj = (ObjectNode) this.keyserver.jsonMapper.readTree(accountValue);
			
			String serviceUserId = accountObj.get("serviceUserId").getTextValue();
			byte[] accountKey = accountObj.get("accountKey").getBinaryValue();
			
			//create InternalToken for UI access
			Token token = new Token(Token.TokenKind.INTERNAL);
			InternalTokenValue tokenValue = new InternalTokenValue(userId, serviceUserId, TokenValue.Role.USER, username, accountKey);
			token.setValue(tokenValue);
			Calendar ttl = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			ttl.add(Calendar.MINUTE, this.keyserver.uiTokenTimeout);
			token.setTTL(ttl);
			this.keyserver.tokenLogic.createToken(token, accountKey);
			
			return new AuthResponse(serviceUserId, token.getB64Token());
		} catch(DatabaseException | CryptoException | NoSuchAlgorithmException | IOException e) {
			throw new KeyserverException(e);
		}
	}
}
