package org.backmeup.keyserver.core;

import static org.backmeup.keyserver.crypto.EncryptionUtils.*;
import static org.backmeup.keyserver.model.KeyserverUtils.toBase64String;

import java.io.IOException;
import java.security.KeyPair;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.crypto.Keyring;
import org.backmeup.keyserver.crypto.PepperApps;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.CryptoException;
import org.backmeup.keyserver.model.EntryNotFoundException;
import org.backmeup.keyserver.model.JsonKeys;
import org.backmeup.keyserver.model.KeyserverEntry;
import org.backmeup.keyserver.model.KeyserverException;
import org.codehaus.jackson.node.ObjectNode;

/**
 * Keyserver implementation module for user specific logic.
 * @author wolfgang
 *
 */
public class DefaultUserLogic {
    private static final MessageFormat USER_ID_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.USER_ID);
    private static final MessageFormat SERVICE_USER_ID_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.SERVICE_USER_ID);
    private static final MessageFormat USERNAME_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.USERNAME);
    private static final MessageFormat ACCOUNT_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.ACCOUNT);
    private static final MessageFormat PROFILE_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.PROFILE);
    private static final MessageFormat INDEX_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.INDEX);
    private static final MessageFormat PUBLIC_KEY_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.PUBLIC_KEY);
    private static final MessageFormat PRIVATE_KEY_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.PRIVATE_KEY);
    
    private DefaultKeyserverImpl keyserver;
    private Keyring keyring;

    public DefaultUserLogic(DefaultKeyserverImpl keyserver) {
        this.keyserver = keyserver;
        this.keyring = this.keyserver.activeKeyring;
    }

    protected Map<String, Object> createBaseUser() throws KeyserverException {
        String userId;
        String serviceUserId;
        byte[] accountKey;

        try {
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
            accountKey = generateSymmetricKey(this.keyring);
            
            // generate public/private key pair for encryption
            KeyPair kp = generateAsymmetricKey(this.keyring);
            
            // [UserId].PublicKey
            this.keyserver.createEntry(fmtKey(PUBLIC_KEY_ENTRY_FMT, userId), kp.getPublic().getEncoded());
            // [UserId].PrivateKey
            byte[] payload = this.keyserver.encryptByteArray(accountKey, PepperApps.PRIVATE_KEY, kp.getPrivate().getEncoded());
            this.keyserver.createEntry(fmtKey(PRIVATE_KEY_ENTRY_FMT, userId), payload);
            
            //[UserId].Index
            String indexKey = generatePassword(this.keyring);
            payload = this.keyserver.encryptString(accountKey, PepperApps.INDEX, indexKey);
            this.keyserver.createEntry(fmtKey(INDEX_ENTRY_FMT, userId), payload);
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
        try {
            KeyserverEntry alreadyExistingUser = this.keyserver.searchForEntry(username, PepperApps.USERNAME, USERNAME_ENTRY_FMT.toPattern());
            if (alreadyExistingUser != null) {
                throw new KeyserverException("duplicate username");
            }
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }
        
        Map<String, Object> baseUser = this.createBaseUser();
        String userId = (String) baseUser.get(JsonKeys.USER_ID);
        String serviceUserId = (String) baseUser.get(JsonKeys.SERVICE_USER_ID);
        byte[] accountKey = (byte[]) baseUser.get(JsonKeys.ACCOUNT_KEY);
        byte[] payload = null;

        try {
            // [Hash(Benutzername)].UserName
            String usernameHash = hashStringWithPepper(this.keyring, username, PepperApps.USERNAME);
            byte[] key = stretchStringWithPepper(this.keyring, username, PepperApps.USERNAME);
            
            payload = this.keyserver.encryptString(key, PepperApps.USERNAME, userId);
            // handle quick re-register problem: User registers again and expired entry was not yet deleted,
            // create new entry with version old+1, all other entries are random number based and thus okay.
            long usernameVersion = 0;
            List<KeyserverEntry> usernameEntries = this.keyserver.db.searchByKey(fmtKey(USERNAME_ENTRY_FMT, usernameHash), true, true);
            for (KeyserverEntry e : usernameEntries) {
                if (e.getVersion() > usernameVersion) {
                    usernameVersion = e.getVersion();
                }
            }
            this.keyserver.createEntry(fmtKey(USERNAME_ENTRY_FMT, usernameHash), payload, null, usernameVersion);
                
            // [UserId].Account
            key = stretchStringWithPepper(this.keyring, username + ";" + password, PepperApps.ACCOUNT);

            ObjectNode valueNode = this.keyserver.jsonMapper.createObjectNode();
            valueNode.put(JsonKeys.SERVICE_USER_ID, serviceUserId);
            valueNode.put(JsonKeys.ACCOUNT_KEY, accountKey);
            
            payload = this.keyserver.encryptString(key, PepperApps.ACCOUNT, valueNode.toString());
            this.keyserver.createEntry(fmtKey(ACCOUNT_ENTRY_FMT, userId), payload);
            
            //[UserId].Profile
            String profile = this.keyserver.defaultProfile;
            payload = this.keyserver.encryptString(accountKey, PepperApps.PROFILE, profile);
            this.keyserver.createEntry(fmtKey(PROFILE_ENTRY_FMT, userId), payload);
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }

        return serviceUserId;
    }

    public AuthResponse registerAnonymous(String decedantServiceUserId, String decedantUserId, byte[] decedantAccountKey) throws KeyserverException {
        Map<String, Object> baseUser = this.createBaseUser();
        String userId = (String) baseUser.get(JsonKeys.USER_ID);
        String serviceUserId = (String) baseUser.get(JsonKeys.SERVICE_USER_ID);
        byte[] accountKey = (byte[]) baseUser.get(JsonKeys.ACCOUNT_KEY);
        
        return this.keyserver.tokenLogic.createInternalForInheritance(userId, serviceUserId, accountKey, decedantUserId, decedantServiceUserId, decedantAccountKey);
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

    public void removeWithUsername(String serviceUserId, String username, byte[] accountKey) throws KeyserverException {
        try {
            //get and delete [Hash(Benutzername)].UserName
            KeyserverEntry usernameEntry = this.keyserver.checkedSearchForEntry(username, PepperApps.USERNAME, USERNAME_ENTRY_FMT.toPattern(), EntryNotFoundException.USERNAME, true);
            //we need the userId to delete the rest of data
            String userId = this.decodeUserId(usernameEntry, username);
            this.keyserver.expireEntry(usernameEntry);
            
            this.remove(serviceUserId, userId, accountKey);
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
    
    public void remove(String serviceUserId, String userId, byte[] accountKey) throws KeyserverException {
        try {            
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
        } catch (DatabaseException e) {
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
            return this.keyserver.tokenLogic.createInternal(userId, serviceUserId, username, accountKey);
        } catch (DatabaseException | CryptoException | IOException e) {
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
    
    protected byte[] getPublicKey(String userId) throws KeyserverException {
        try {
            KeyserverEntry pubkeyEntry = this.keyserver.checkedGetEntry(fmtKey(PUBLIC_KEY_ENTRY_FMT, userId), EntryNotFoundException.PUBLIC_KEY);
            return pubkeyEntry.getValue();
        } catch (DatabaseException e) {
            throw new KeyserverException(e);
        }
    }
    
    protected byte[] getPrivateKey(String userId, byte[] accountKey) throws KeyserverException {
        try {
            KeyserverEntry privkeyEntry = this.keyserver.checkedGetEntry(fmtKey(PRIVATE_KEY_ENTRY_FMT, userId), EntryNotFoundException.PRIVATE_KEY);
            return this.keyserver.decryptByteArray(accountKey, PepperApps.PRIVATE_KEY, privkeyEntry.getValue());
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
}