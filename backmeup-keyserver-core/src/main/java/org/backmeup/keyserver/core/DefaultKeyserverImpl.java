package org.backmeup.keyserver.core;

import static org.backmeup.keyserver.core.KeyserverUtils.hashByteArrayWithPepper;
import static org.backmeup.keyserver.core.KeyserverUtils.toBase64String;

import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.backmeup.keyserver.core.config.Configuration;
import org.backmeup.keyserver.core.config.KeyringConfiguration;
import org.backmeup.keyserver.core.crypto.CryptoException;
import org.backmeup.keyserver.core.crypto.Keyring;
import org.backmeup.keyserver.core.db.Database;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.KeyserverEntry;
import org.backmeup.keyserver.model.Token;
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

    public DefaultKeyserverImpl() throws KeyserverException {
        this.loadKeyrings();

        this.serviceId = Configuration.getProperty("backmeup.service.id");
        this.servicePassword = Configuration.getProperty("backmeup.service.password");
        this.uiTokenTimeout = Integer.parseInt(Configuration.getProperty("backmeup.keyserver.uiTokenTimeout"));

        this.db = new org.backmeup.keyserver.core.db.derby.DerbyDatabaseImpl();
        try {
            this.db.connect();
        } catch (DatabaseException e) {
            throw new KeyserverException("keyserver cannot connect to database", e);
        }

        this.appLogic = new DefaultAppLogic(this);
        this.userLogic = new DefaultUserLogic(this);
        this.tokenLogic = new DefaultTokenLogic(this);
    }

    private void loadKeyrings() {        
        for(Keyring k : KeyringConfiguration.getKeyrings()) {
            this.keyrings.put(k.getKeyringId(), k);
        }

        // set highest one as active keyring
        this.activeKeyring = this.keyrings.get(this.keyrings.firstKey());
    }

    protected KeyserverEntry createEntry(String key, byte[] payload, Calendar ttl) throws DatabaseException {
        KeyserverEntry entry = new KeyserverEntry(key);
        if (payload != null) {
            entry.setValue(payload);
        }
        
        entry.setTTL(ttl);
        
        this.db.putEntry(entry);
        return entry;
    }
    
    protected KeyserverEntry createEntry(String key, byte[] payload) throws DatabaseException {
        return this.createEntry(key, payload, null);
    }
    
    protected KeyserverEntry searchForEntry(String[] hashInputs, String[] pepperApplications, String keyPattern) throws CryptoException, DatabaseException {
        MessageFormat key = new MessageFormat(keyPattern);

        for (Keyring k : this.keyrings.values()) {
            String[] hashes = new String[hashInputs.length];
            for (int i = 0; i < hashInputs.length; i++) {
                hashes[i] = KeyserverUtils.hashStringWithPepper(k, hashInputs[i], pepperApplications[i]);
            }

            KeyserverEntry entry = this.db.getEntry(key.format(hashes));
            if (entry != null) {
                return entry;
            }
        }

        return null;
    }

    protected KeyserverEntry searchForEntry(String hashInput, String pepperApplication, String keyPattern) throws CryptoException, DatabaseException {
        return this.searchForEntry(new String[] { hashInput }, new String[] { pepperApplication }, keyPattern);
    }

    protected KeyserverEntry searchForEntry(byte[][] hashInputs, String[] pepperApplications, String keyPattern) throws CryptoException, DatabaseException {
        MessageFormat key = new MessageFormat(keyPattern);

        for (Keyring k : this.keyrings.values()) {
            String[] hashes = new String[hashInputs.length];
            for (int i = 0; i < hashInputs.length; i++) {
                hashes[i] = toBase64String(KeyserverUtils.hashByteArrayWithPepper(k, hashInputs[i], pepperApplications[i]));
            }

            KeyserverEntry entry = this.db.getEntry(key.format(hashes));
            if (entry != null) {
                return entry;
            }
        }

        return null;
    }

    protected KeyserverEntry searchForEntry(byte[] hashInput, String pepperApplication, String keyPattern) throws CryptoException, DatabaseException {
        return this.searchForEntry(new byte[][] { hashInput }, new String[] { pepperApplication }, keyPattern);
    }
    
    protected KeyserverEntry checkedGetEntry(String key, String notFoundMessage, boolean checkMigration) throws DatabaseException, EntryNotFoundException {
        KeyserverEntry entry = this.db.getEntry(key);
        if (entry == null) {
            throw new EntryNotFoundException(notFoundMessage);
        }

        if (checkMigration && (entry.getKeyringId() < this.activeKeyring.getKeyringId())) {
            // TODO: migrate Entry
        }
        return entry;
    }
    
    protected KeyserverEntry checkedGetEntry(String key, String notFoundMessage) throws DatabaseException, EntryNotFoundException {
        return this.checkedGetEntry(key, notFoundMessage, true);
    }
    
    protected KeyserverEntry checkedSearchForEntry(String hashInput, String pepperApplication, String keyPattern, String notFoundMessage, boolean checkMigration) throws CryptoException, DatabaseException, EntryNotFoundException {
        KeyserverEntry entry = this.searchForEntry(hashInput, pepperApplication, keyPattern);
        if (entry == null) {
            throw new EntryNotFoundException(notFoundMessage);
        }

        if (checkMigration && (entry.getKeyringId() < this.activeKeyring.getKeyringId())) {
            // TODO: migrate Entry
        }
        
        return entry;
    }
    
    protected KeyserverEntry checkedSearchForEntry(byte[] hashInput, String pepperApplication, String keyPattern, String notFoundMessage, boolean checkMigration) throws CryptoException, DatabaseException, EntryNotFoundException {
        KeyserverEntry entry = this.searchForEntry(hashInput, pepperApplication, keyPattern);
        if (entry == null) {
            throw new EntryNotFoundException(notFoundMessage);
        }

        if (checkMigration && (entry.getKeyringId() < this.activeKeyring.getKeyringId())) {
            // TODO: migrate Entry
        }
        
        return entry;
    }
    
    protected void expireEntry(KeyserverEntry entry) throws DatabaseException {
        entry.expire();
        this.db.updateTTL(entry);
    }
    
    protected void updateEntry(KeyserverEntry entry, byte[] value) throws DatabaseException {
        this.expireEntry(entry);
        entry.setValue(value);
        this.db.putEntry(entry);
    }
    
    protected String decryptString(byte[] key, String pepperApplication, byte[] value) throws CryptoException {
        return KeyserverUtils.decryptString(this.activeKeyring, hashByteArrayWithPepper(this.activeKeyring, key, pepperApplication), value);
    }
    
    protected byte[] encryptString(byte[] key, String pepperApplication, String value) throws CryptoException { 
        return KeyserverUtils.encryptString(this.activeKeyring, hashByteArrayWithPepper(this.activeKeyring, key, pepperApplication), value);
    }
    
    protected byte[] decryptByteArray(byte[] key, String pepperApplication, byte[] value) throws CryptoException {
        return KeyserverUtils.decryptByteArray(this.activeKeyring, hashByteArrayWithPepper(this.activeKeyring, key, pepperApplication), value);
    }
    
    protected byte[] encryptByteArray(byte[] key, String pepperApplication, byte[] value) throws CryptoException {
        return KeyserverUtils.encryptByteArray(this.activeKeyring, hashByteArrayWithPepper(this.activeKeyring, key, pepperApplication), value);
    }

    //=========================================================================
    // User logic
    //=========================================================================

    @Override
    public String registerUser(String username, String password) throws KeyserverException {
        return this.userLogic.register(username, password);
    }

    @Override
    public String registerAnonoumysUser(String username, String password) throws KeyserverException {
        return this.userLogic.registerAnonoumys(username, password);
    }
    
    @Override
    public void removeUser(String serviceUserId, String username) throws KeyserverException {
        this.userLogic.remove(serviceUserId, username, null);
    }
    
    @Override
    public void removeUser(String serviceUserId, String username, byte[] accountKey) throws KeyserverException {
        this.userLogic.remove(serviceUserId, username, accountKey);
    }
    
    @Override
    public void changeUserPassword(String userId, String username, String oldPassword, String newPassword) throws KeyserverException {
        this.userLogic.changePassword(userId, username, oldPassword, newPassword);
    }

    @Override
    public AuthResponse authenticateUserWithPassword(String username, String password) throws KeyserverException {
        return this.userLogic.authenticateWithPassword(username, password);
    }
    
    @Override
    public void setProfile(String userId, byte[] accountKey, String profile) throws KeyserverException {       
        this.userLogic.setProfile(userId, accountKey, profile);
    }
    
    @Override
    public String getProfile(String userId, byte[] accountKey) throws KeyserverException {
        return this.userLogic.getProfile(userId, accountKey);
    }
    
    @Override
    public String getIndexKey(String userId, byte[] accountKey) throws KeyserverException {
        return this.userLogic.getIndexKey(userId, accountKey);
    }
    
    //=========================================================================
    // Token logic
    //=========================================================================
    
    @Override
    public AuthResponse authenticateWithInternalToken(String tokenHash) throws KeyserverException {
        return this.tokenLogic.authenticateWithInternal(tokenHash);
    }
    
    @Override
    public List<Token> listTokens(String userId, byte[] accountKey, Token.Kind kind) throws KeyserverException {
        return this.tokenLogic.listTokens(userId, accountKey, kind);
    }
    
    @Override
    public void revokeToken(Token.Kind kind, String tokenHash) throws KeyserverException {
        this.tokenLogic.revoke(new Token(kind, tokenHash));
    }

    //=========================================================================
    // App logic
    //=========================================================================
    
    @Override
    public App registerApp(App.Approle role) throws KeyserverException {
        return this.appLogic.register(role);
    }
    
    @Override
    public List<App> listApps(String servicePassword) throws KeyserverException {
        return this.appLogic.listApps(servicePassword);
    }

    @Override
    public void removeApp(String appId) throws KeyserverException {
        this.appLogic.remove(appId);
    }

    @Override
    public App authenticateApp(String appId, String appKey) throws KeyserverException {
        return this.appLogic.authenticate(appId, appKey);
    }
}
