package org.backmeup.keyserver.core;

import static org.backmeup.keyserver.core.KeyserverUtils.decryptString;
import static org.backmeup.keyserver.core.KeyserverUtils.encryptString;
import static org.backmeup.keyserver.core.KeyserverUtils.fmtKey;
import static org.backmeup.keyserver.core.KeyserverUtils.generateKey;
import static org.backmeup.keyserver.core.KeyserverUtils.hashByteArrayWithPepper;
import static org.backmeup.keyserver.core.KeyserverUtils.hashStringWithPepper;
import static org.backmeup.keyserver.core.KeyserverUtils.stretchStringWithPepper;
import static org.backmeup.keyserver.core.KeyserverUtils.toBase64String;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.HashMap;
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

                KeyserverEntry uid = this.db.getEntry(fmtKey(USER_ID_ENTRY_FMT, userId));
                KeyserverEntry suid = this.db.getEntry(fmtKey(SERVICE_USER_ID_ENTRY_FMT, serviceUserId));
                collission = (uid != null) || (suid != null);
            } while (collission);

            // [UserId].UserId
            KeyserverEntry ke = new KeyserverEntry(fmtKey(USER_ID_ENTRY_FMT, userId));
            db.putEntry(ke);

            // [ServiceUserId].ServiceUserId
            ke = new KeyserverEntry(fmtKey(SERVICE_USER_ID_ENTRY_FMT, serviceUserId));
            db.putEntry(ke);
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }

        Map<String, String> ret = new HashMap<>();
        ret.put(JsonKeys.USER_ID, userId);
        ret.put(JsonKeys.SERVICE_USER_ID, serviceUserId);
        return ret;
    }

    public String registerUser(String username, String password) throws KeyserverException {
        Map<String, String> ids = this.createBaseUser(username, password);
        String userId = ids.get(JsonKeys.USER_ID);
        String serviceUserId = ids.get(JsonKeys.SERVICE_USER_ID);

        try {
            // [Hash(Benutzername)].UserName
            String usernameHash = hashStringWithPepper(this.keyring, username, PepperApps.USERNAME);

            byte[] key = stretchStringWithPepper(this.keyring, username, PepperApps.USERNAME);
            byte[] payload = encryptString(this.keyring, hashByteArrayWithPepper(this.keyring, key, PepperApps.USERNAME), userId);

            KeyserverEntry ke = new KeyserverEntry(fmtKey(USERNAME_ENTRY_FMT, usernameHash));
            ke.setValue(payload);
            db.putEntry(ke);

            // [UserId].Account
            byte[] accountKey = generateKey(this.keyring);
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
        Map<String, String> ids = this.createBaseUser(username, password);
        // String userId = ids.get(JsonKeys.USER_ID);
        String serviceUserId = ids.get(JsonKeys.SERVICE_USER_ID);

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