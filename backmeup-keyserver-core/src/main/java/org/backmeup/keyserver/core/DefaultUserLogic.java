package org.backmeup.keyserver.core;

import static org.backmeup.keyserver.core.KeyserverUtils.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.backmeup.keyserver.core.crypto.CryptoException;
import org.backmeup.keyserver.core.crypto.Keyring;
import org.backmeup.keyserver.core.crypto.PepperApps;
import org.backmeup.keyserver.core.db.Database;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.JsonKeys;
import org.backmeup.keyserver.model.KeyserverEntry;
import org.backmeup.keyserver.model.Token;
import org.backmeup.keyserver.model.TokenValue;
import org.codehaus.jackson.node.ObjectNode;

public class DefaultUserLogic {
    private static final MessageFormat USER_ID_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.USER_ID);
    private static final MessageFormat SERVICE_USER_ID_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.USER_ID);
    private static final MessageFormat USERNAME_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.USERNAME);
    private static final MessageFormat ACCOUNT_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.ACCOUNT);
    private static final MessageFormat ACCOUNT_PUBKKEY_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.ACCOUNT_PUBK_KEY);
    
    private DefaultKeyserverImpl keyserver;
    private Keyring keyring;
    private Database db;

    public DefaultUserLogic(DefaultKeyserverImpl keyserver) {
        this.keyserver = keyserver;
        this.keyring = this.keyserver.activeKeyring;
        this.db = this.keyserver.db;
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

                List<KeyserverEntry> uids = this.db.searchByKey(fmtKey(USER_ID_ENTRY_FMT, userId), true, true);
                List<KeyserverEntry> suids = this.db.searchByKey(fmtKey(SERVICE_USER_ID_ENTRY_FMT, serviceUserId), true, true);
                collission = (!uids.isEmpty()) || (!suids.isEmpty());
            } while (collission);

            // [UserId].UserId
            KeyserverEntry ke = new KeyserverEntry(fmtKey(USER_ID_ENTRY_FMT, userId));
            db.putEntry(ke);

            // [ServiceUserId].ServiceUserId
            ke = new KeyserverEntry(fmtKey(SERVICE_USER_ID_ENTRY_FMT, serviceUserId));
            db.putEntry(ke);
            
            // generate accountKey, which is saved later in [UserId].Account or in [Hash(UserKey)].InternalToken
            accountKey = generateKey(this.keyring);
            
            // [UserId].Account.PKey
            byte[] pkkey = generateKey(this.keyring);
            ke = new KeyserverEntry(fmtKey(ACCOUNT_PUBKKEY_ENTRY_FMT, userId));
            byte[] payload = encryptByteArray(this.keyring, hashByteArrayWithPepper(this.keyring, accountKey, PepperApps.ACCOUNT_PUBK_KEY), pkkey);
            ke.setValue(payload);
            db.putEntry(ke);
            
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }

        Map<String, Object> ret = new HashMap<>();
        ret.put(JsonKeys.USER_ID, userId);
        ret.put(JsonKeys.SERVICE_USER_ID, serviceUserId);
        ret.put(JsonKeys.ACCOUNT_KEY, accountKey);
        return ret;
    }

    public String registerUser(String username, String password) throws KeyserverException {
        Map<String, Object> baseUser = this.createBaseUser(username, password);
        String userId = (String) baseUser.get(JsonKeys.USER_ID);
        String serviceUserId = (String) baseUser.get(JsonKeys.SERVICE_USER_ID);

        try {
            // [Hash(Benutzername)].UserName
            String usernameHash = hashStringWithPepper(this.keyring, username, PepperApps.USERNAME);

            byte[] key = stretchStringWithPepper(this.keyring, username, PepperApps.USERNAME);
            byte[] payload = encryptString(this.keyring, hashByteArrayWithPepper(this.keyring, key, PepperApps.USERNAME), userId);

            KeyserverEntry ke = new KeyserverEntry(fmtKey(USERNAME_ENTRY_FMT, usernameHash));
            ke.setValue(payload);
            db.putEntry(ke);

            // [UserId].Account
            byte[] accountKey = (byte[]) baseUser.get(JsonKeys.ACCOUNT_KEY);
            key = stretchStringWithPepper(this.keyring, username + ";" + password, PepperApps.ACCOUNT);

            ObjectNode valueNode = this.keyserver.jsonMapper.createObjectNode();
            valueNode.put(JsonKeys.SERVICE_USER_ID, serviceUserId);
            valueNode.put(JsonKeys.ACCOUNT_KEY, accountKey);
            payload = encryptString(this.keyring, hashByteArrayWithPepper(this.keyring, key, PepperApps.ACCOUNT), valueNode.toString());

            ke = new KeyserverEntry(fmtKey(ACCOUNT_ENTRY_FMT, userId));
            ke.setValue(payload);
            db.putEntry(ke);
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }

        return serviceUserId;
    }

    public String registerAnonoumysUser(String username, String password) throws KeyserverException {
        Map<String, Object> baseUser = this.createBaseUser(username, password);
        String serviceUserId = (String) baseUser.get(JsonKeys.SERVICE_USER_ID);

        return serviceUserId;
    }

    protected String getUserId(String username) throws KeyserverException {
        try {
            KeyserverEntry usernameEntry = this.keyserver.searchForEntry(username, PepperApps.USERNAME, USERNAME_ENTRY_FMT.toPattern());
            if (usernameEntry == null) {
                throw new EntryNotFoundException(EntryNotFoundException.USERNAME);
            }

            if (usernameEntry.getKeyringId() < this.keyring.getKeyringId()) {
                // TODO: migrate Entry
            }

            byte[] key = stretchStringWithPepper(this.keyring, username, PepperApps.USERNAME);
            return decryptString(this.keyring, hashByteArrayWithPepper(this.keyring, key, PepperApps.USERNAME), usernameEntry.getValue());
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
    
    protected boolean checkServiceUserId(String serviceUserId) throws DatabaseException {
        return this.db.getEntry(fmtKey(SERVICE_USER_ID_ENTRY_FMT, serviceUserId)) != null;
    }
    
    protected byte[] getPubKKey(String userId, byte[] accountKey) throws KeyserverException {
        try {
            KeyserverEntry pubkkeyEntry = this.db.getEntry(fmtKey(ACCOUNT_PUBKKEY_ENTRY_FMT, userId));
            if (pubkkeyEntry == null) {
                throw new EntryNotFoundException(EntryNotFoundException.ACCOUNT);
            }

            if (pubkkeyEntry.getKeyringId() < this.keyring.getKeyringId()) {
                // TODO: migrate Entry
            }

            return decryptByteArray(this.keyring, hashByteArrayWithPepper(this.keyring, accountKey, PepperApps.ACCOUNT_PUBK_KEY), pubkkeyEntry.getValue());
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }

    public void remove(String username) throws KeyserverException {
        // TODO
        throw new UnsupportedOperationException("not implemented yet");
        /*
         * String userId = this.getUserId(username);
         * 
         * 
         * try { KeyserverEntry appEntry = this.db.getEntry(appId+".App"); if
         * (appEntry == null) { throw new KeyserverException("appId not found");
         * }
         * 
         * appEntry.expire(); this.db.updateTTL(appEntry); }
         * catch(DatabaseException e) { throw new KeyserverException(e); }
         */
    }
    
    public void changePassword(String username, String oldPassword, String newPassword) throws KeyserverException {
        throw new UnsupportedOperationException("not implemented yet");
        //TODO
    }

    public AuthResponse authenticateWithPassword(String username, String password) throws KeyserverException {
        String userId = this.getUserId(username);

        try {
            KeyserverEntry accountEntry = this.db.getEntry(fmtKey(ACCOUNT_ENTRY_FMT, userId));
            if (accountEntry == null) {
                throw new EntryNotFoundException(EntryNotFoundException.ACCOUNT);
            }

            if (accountEntry.getKeyringId() < this.keyring.getKeyringId()) {
                // TODO: migrate Entry
            }

            byte[] key = stretchStringWithPepper(this.keyring, username + ";" + password, PepperApps.ACCOUNT);
            String accountValue = decryptString(this.keyring, hashByteArrayWithPepper(this.keyring, key, PepperApps.ACCOUNT), accountEntry.getValue());
            ObjectNode accountObj = (ObjectNode) this.keyserver.jsonMapper.readTree(accountValue);

            String serviceUserId = accountObj.get(JsonKeys.SERVICE_USER_ID).getTextValue();
            byte[] accountKey = accountObj.get(JsonKeys.ACCOUNT_KEY).getBinaryValue();

            // create InternalToken for UI access
            Token token = new Token(Token.Kind.INTERNAL);
            TokenValue tokenValue = new TokenValue(userId, serviceUserId, TokenValue.Role.USER);
            tokenValue.putValue(JsonKeys.USERNAME, username);
            tokenValue.putValue(JsonKeys.ACCOUNT_KEY, accountKey);
            token.setValue(tokenValue);
            
            Calendar ttl = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            ttl.add(Calendar.MINUTE, this.keyserver.uiTokenTimeout);
            token.setTTL(ttl);
            
            this.keyserver.tokenLogic.createToken(token, accountKey);

            return new AuthResponse(token);
        } catch (DatabaseException | CryptoException | IOException e) {
            throw new KeyserverException(e);
        }
    }
}