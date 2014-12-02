package org.backmeup.keyserver.core;

import static org.backmeup.keyserver.core.KeyserverUtils.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.backmeup.keyserver.core.crypto.CryptoException;
import org.backmeup.keyserver.core.crypto.Keyring;
import org.backmeup.keyserver.core.crypto.PepperApps;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.JsonKeys;
import org.backmeup.keyserver.model.KeyserverEntry;
import org.backmeup.keyserver.model.Token;
import org.codehaus.jackson.node.ObjectNode;

public class DefaultUserLogic {
    private static final MessageFormat USER_ID_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.USER_ID);
    private static final MessageFormat SERVICE_USER_ID_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.SERVICE_USER_ID);
    private static final MessageFormat USERNAME_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.USERNAME);
    private static final MessageFormat ACCOUNT_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.ACCOUNT);
    private static final MessageFormat ACCOUNT_PUBKKEY_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.ACCOUNT_PUBK_KEY);
    private static final MessageFormat PROFILE_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.PROFILE);
    private static final MessageFormat INDEX_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.INDEX);
    
    private DefaultKeyserverImpl keyserver;
    private Keyring keyring;

    public DefaultUserLogic(DefaultKeyserverImpl keyserver) {
        this.keyserver = keyserver;
        this.keyring = this.keyserver.activeKeyring;
    }

    protected Map<String, Object> createBaseUser(String username, String password) throws KeyserverException {
        String userId;
        String serviceUserId;
        byte[] accountKey;

        try {
            KeyserverEntry alreadyExistingUser = this.keyserver.searchForEntry(username, PepperApps.USERNAME, USERNAME_ENTRY_FMT.toPattern());
            if (alreadyExistingUser != null) {
                throw new KeyserverException("duplicate username");
            }

            boolean collission = false;
            do {
                byte[] userKey = new byte[32];
                this.keyserver.random.nextBytes(userKey);

                userId = toBase64String(hashByteArrayWithPepper(this.keyring, userKey, PepperApps.USER_ID));
                serviceUserId = toBase64String(hashByteArrayWithPepper(this.keyring, userKey, PepperApps.SERVICE_USER_ID));

                List<KeyserverEntry> uids = this.keyserver.db.searchByKey(fmtKey(USER_ID_ENTRY_FMT, userId), true, true);
                List<KeyserverEntry> suids = this.keyserver.db.searchByKey(fmtKey(SERVICE_USER_ID_ENTRY_FMT, serviceUserId), true, true);
                collission = (!uids.isEmpty()) || (!suids.isEmpty());
            } while (collission);

            // [UserId].UserId
            this.keyserver.createEntry(fmtKey(USER_ID_ENTRY_FMT, userId), null);

            // [ServiceUserId].ServiceUserId
            this.keyserver.createEntry(fmtKey(SERVICE_USER_ID_ENTRY_FMT, serviceUserId), null);
            
            // generate accountKey, which is saved later in [UserId].Account or in [Hash(UserKey)].InternalToken
            accountKey = generateKey(this.keyring);
            
            // [UserId].Account.PKey
            byte[] pkkey = generateKey(this.keyring);
            byte[] payload = this.keyserver.encryptByteArray(accountKey, PepperApps.ACCOUNT_PUBK_KEY, pkkey);
            this.keyserver.createEntry(fmtKey(ACCOUNT_PUBKKEY_ENTRY_FMT, userId), payload);
            
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }

        Map<String, Object> ret = new HashMap<>();
        ret.put(JsonKeys.USER_ID, userId);
        ret.put(JsonKeys.SERVICE_USER_ID, serviceUserId);
        ret.put(JsonKeys.ACCOUNT_KEY, accountKey);
        return ret;
    }

    public String register(String username, String password) throws KeyserverException {
        Map<String, Object> baseUser = this.createBaseUser(username, password);
        String userId = (String) baseUser.get(JsonKeys.USER_ID);
        String serviceUserId = (String) baseUser.get(JsonKeys.SERVICE_USER_ID);
        byte[] accountKey = (byte[]) baseUser.get(JsonKeys.ACCOUNT_KEY);
        byte[] payload = null;

        try {
            // [Hash(Benutzername)].UserName
            String usernameHash = hashStringWithPepper(this.keyring, username, PepperApps.USERNAME);
            byte[] key = stretchStringWithPepper(this.keyring, username, PepperApps.USERNAME);
            
            payload = this.keyserver.encryptString(key, PepperApps.USERNAME, userId);
            this.keyserver.createEntry(fmtKey(USERNAME_ENTRY_FMT, usernameHash), payload);

            // [UserId].Account
            key = stretchStringWithPepper(this.keyring, username + ";" + password, PepperApps.ACCOUNT);

            ObjectNode valueNode = this.keyserver.jsonMapper.createObjectNode();
            valueNode.put(JsonKeys.SERVICE_USER_ID, serviceUserId);
            valueNode.put(JsonKeys.ACCOUNT_KEY, accountKey);
            
            payload = this.keyserver.encryptString(key, PepperApps.ACCOUNT, valueNode.toString());
            this.keyserver.createEntry(fmtKey(ACCOUNT_ENTRY_FMT, userId), payload);
            
            //[UserId].Profile
            //TODO: default profile structure?
            String profile = "";
            payload = this.keyserver.encryptString(accountKey, PepperApps.PROFILE, profile);
            this.keyserver.createEntry(fmtKey(PROFILE_ENTRY_FMT, userId), payload);
            
            //[UserId].Index
            String indexKey = generatePassword(this.keyring);
            payload = this.keyserver.encryptString(accountKey, PepperApps.INDEX, indexKey);
            this.keyserver.createEntry(fmtKey(INDEX_ENTRY_FMT, userId), payload);
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }

        return serviceUserId;
    }

    public String registerAnonoumys(String username, String password) throws KeyserverException {
        Map<String, Object> baseUser = this.createBaseUser(username, password);
        String serviceUserId = (String) baseUser.get(JsonKeys.SERVICE_USER_ID);

        return serviceUserId;
    }

    protected String getUserId(String username) throws KeyserverException {
        try {
            KeyserverEntry usernameEntry = this.keyserver.checkedSearchForEntry(username, PepperApps.USERNAME, USERNAME_ENTRY_FMT.toPattern(), EntryNotFoundException.USERNAME, true);
            return this.decodeUserId(usernameEntry, username);
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
    
    protected String decodeUserId(KeyserverEntry usernameEntry, String username) throws CryptoException {
        byte[] key = stretchStringWithPepper(this.keyring, username, PepperApps.USERNAME);
        return this.keyserver.decryptString(key, PepperApps.USERNAME, usernameEntry.getValue());
    }
    
    protected boolean checkServiceUserId(String serviceUserId) throws DatabaseException {
        return this.keyserver.db.getEntry(fmtKey(SERVICE_USER_ID_ENTRY_FMT, serviceUserId)) != null;
    }

    public void remove(String serviceUserId, String username, byte[] accountKey) throws KeyserverException {
        try {
            //get and delete [Hash(Benutzername)].UserName
            KeyserverEntry usernameEntry = this.keyserver.checkedSearchForEntry(username, PepperApps.USERNAME, USERNAME_ENTRY_FMT.toPattern(), EntryNotFoundException.USERNAME, true);
            //we need the userId to delete the rest of data
            String userId = this.decodeUserId(usernameEntry, username);
            this.keyserver.expireEntry(usernameEntry);
            
            //get and delete [ServiceUserId].ServiceUserId
            KeyserverEntry serviceUserIdEntry = this.keyserver.checkedGetEntry(fmtKey(SERVICE_USER_ID_ENTRY_FMT, serviceUserId), EntryNotFoundException.SERVICE_USER_ID);
            this.keyserver.expireEntry(serviceUserIdEntry);
            
            //if accountKey is provided - delete all tokens of user
            //else all token annotations will be deleted in next step but actual tokens will remain in database
            //they will be deleted at later usage
            if (accountKey != null) {
                this.keyserver.tokenLogic.removeTokens(userId, accountKey);
            }
            
            //get all [UserId].% entries and delete them
            List<KeyserverEntry> userIdEntries = this.keyserver.db.searchByKey(userId+".%", true, false);
            for (KeyserverEntry entry : userIdEntries) {
                this.keyserver.expireEntry(entry);
            }
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }

    }
    
    public void changePassword(String userId, String username, String oldPassword, String newPassword) throws KeyserverException {
        try {
            KeyserverEntry accountEntry = this.keyserver.checkedGetEntry(fmtKey(ACCOUNT_ENTRY_FMT, userId), EntryNotFoundException.ACCOUNT);

            //decrypt with the old one
            byte[] key = stretchStringWithPepper(this.keyring, username + ";" + oldPassword, PepperApps.ACCOUNT);
            String accountValue = this.keyserver.decryptString(key, PepperApps.ACCOUNT, accountEntry.getValue());
            
            //encrypt with the one
            key = stretchStringWithPepper(this.keyring, username + ";" + newPassword, PepperApps.ACCOUNT);
            byte[] payload = this.keyserver.encryptString(key, PepperApps.ACCOUNT, accountValue);
            
            this.keyserver.updateEntry(accountEntry, payload);
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }

    public AuthResponse authenticateWithPassword(String username, String password) throws KeyserverException {
        String userId = this.getUserId(username);

        try {
            KeyserverEntry accountEntry = this.keyserver.checkedGetEntry(fmtKey(ACCOUNT_ENTRY_FMT, userId), EntryNotFoundException.ACCOUNT);

            byte[] key = stretchStringWithPepper(this.keyring, username + ";" + password, PepperApps.ACCOUNT);
            String accountValue = this.keyserver.decryptString(key, PepperApps.ACCOUNT, accountEntry.getValue());
            ObjectNode accountObj = (ObjectNode) this.keyserver.jsonMapper.readTree(accountValue);

            String serviceUserId = accountObj.get(JsonKeys.SERVICE_USER_ID).getTextValue();
            byte[] accountKey = accountObj.get(JsonKeys.ACCOUNT_KEY).getBinaryValue();

            // create InternalToken for UI access
            Token token = this.keyserver.tokenLogic.createInternal(userId, serviceUserId, username, accountKey);

            return new AuthResponse(token);
        } catch (DatabaseException | CryptoException | IOException e) {
            throw new KeyserverException(e);
        }
    }
    
    protected byte[] getPubKKey(String userId, byte[] accountKey) throws KeyserverException {
        try {
            KeyserverEntry pubkkeyEntry = this.keyserver.checkedGetEntry(fmtKey(ACCOUNT_PUBKKEY_ENTRY_FMT, userId), EntryNotFoundException.ACCOUNT);
            return this.keyserver.decryptByteArray(accountKey, PepperApps.ACCOUNT_PUBK_KEY, pubkkeyEntry.getValue());
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
    
    public void setProfile(String userId, byte[] accountKey, String profile) throws KeyserverException {       
        try {          
            KeyserverEntry profileEntry = this.keyserver.checkedGetEntry(fmtKey(PROFILE_ENTRY_FMT, userId), EntryNotFoundException.PROFILE);
            
            byte[] payload = this.keyserver.encryptString(accountKey, PepperApps.PROFILE, profile);
            this.keyserver.updateEntry(profileEntry, payload);
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
    
    public String getProfile(String userId, byte[] accountKey) throws KeyserverException {      
        try {
            KeyserverEntry profileEntry = this.keyserver.checkedGetEntry(fmtKey(PROFILE_ENTRY_FMT, userId), EntryNotFoundException.PROFILE);
            return this.keyserver.decryptString(accountKey, PepperApps.PROFILE, profileEntry.getValue());
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
    
    public String getIndexKey(String userId, byte[] accountKey) throws KeyserverException {      
        try {
            KeyserverEntry indexEntry = this.keyserver.checkedGetEntry(fmtKey(INDEX_ENTRY_FMT, userId), EntryNotFoundException.INDEX);
            return this.keyserver.decryptString(accountKey, PepperApps.INDEX, indexEntry.getValue());
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
}