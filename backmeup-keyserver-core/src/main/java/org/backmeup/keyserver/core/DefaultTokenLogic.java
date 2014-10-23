package org.backmeup.keyserver.core;

import static org.backmeup.keyserver.core.KeyserverUtils.decryptString;
import static org.backmeup.keyserver.core.KeyserverUtils.encryptString;
import static org.backmeup.keyserver.core.KeyserverUtils.fmtKey;
import static org.backmeup.keyserver.core.KeyserverUtils.generateKey;
import static org.backmeup.keyserver.core.KeyserverUtils.hashByteArrayWithPepper;
import static org.backmeup.keyserver.core.KeyserverUtils.toBase64String;

import java.text.MessageFormat;
import java.util.List;

import org.backmeup.keyserver.core.crypto.CryptoException;
import org.backmeup.keyserver.core.crypto.Keyring;
import org.backmeup.keyserver.core.db.Database;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.model.InternalTokenValue;
import org.backmeup.keyserver.model.JsonKeys;
import org.backmeup.keyserver.model.KeyserverEntry;
import org.backmeup.keyserver.model.Token;
import org.backmeup.keyserver.model.TokenValue;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

public class DefaultTokenLogic {
    private static final MessageFormat TK_ENTRY_FORMAT = new MessageFormat("{0}.{1}");
    private static final MessageFormat DESC_ENTRY_FORMAT = new MessageFormat("{0}.Account.{1}.{2}");
    
    private DefaultKeyserverImpl keyserver;
    private Keyring keyring;
    private Database db;

    public DefaultTokenLogic(DefaultKeyserverImpl keyserver) {
        this.keyserver = keyserver;
        this.keyring = this.keyserver.activeKeyring;
        this.db = this.keyserver.db;
    }
    
    private static String tkKey(String tokenHash, String tokenKindApp) {
        return fmtKey(TK_ENTRY_FORMAT, tokenHash, tokenKindApp);
    }
    
    private static String descKey(String userId, String tokenKindApp, String tokenHash) {
        return fmtKey(DESC_ENTRY_FORMAT, userId, tokenKindApp, tokenHash);
    }

    public void createToken(Token token, byte[] accountKey) throws KeyserverException {
        String tokenHash = null;
        byte[] tokenKey = null;
        String tokenKindApp = token.getKind().getApplication();

        try {
            boolean collission = false;
            do {
                tokenKey = generateKey(this.keyring);
                tokenHash = toBase64String(hashByteArrayWithPepper(this.keyring, tokenKey, tokenKindApp));

                KeyserverEntry t = this.db.getEntry(tkKey(tokenHash, tokenKindApp));
                collission = (t != null);
            } while (collission);

            token.setToken(tokenKey);

            // [Hash(Token)].InternalToken
            KeyserverEntry ke = new KeyserverEntry(tkKey(tokenHash, tokenKindApp));
            byte[] payload = encryptString(this.keyring, hashByteArrayWithPepper(this.keyring, tokenKey, tokenKindApp),
                    this.mapTokenValueToJson(token.getValue()));
            ke.setValue(payload);
            ke.setTTL(token.getTTL());
            this.db.putEntry(ke);

            if (token.getAnnotation() != null) {
                // [UserId].Account.InternalToken.[Hash(Token)]
                ke = new KeyserverEntry(descKey(token.getValue().getUserId(), tokenKindApp, tokenHash));
                payload = encryptString(this.keyring, hashByteArrayWithPepper(this.keyring, accountKey, tokenKindApp), this.mapTokenToJson(token));
                ke.setValue(payload);
                ke.setTTL(token.getTTL());
                this.db.putEntry(ke);
            }
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }
    }

    protected void retrieveTokenValue(Token token) throws KeyserverException {
        String tokenKindApp = token.getKind().getApplication();

        try {
            KeyserverEntry tokenEntry = this.keyserver.searchForEntry(token.getToken(), tokenKindApp, "{0}."+tokenKindApp);
            if (tokenEntry == null) {
                throw new KeyserverException("token not found");
            }

            if (tokenEntry.getKeyringId() < this.keyring.getKeyringId()) {
                // TODO: migrate Entry
            }

            String tokenValue = decryptString(this.keyring, hashByteArrayWithPepper(this.keyring, token.getToken(), tokenKindApp),
                    tokenEntry.getValue());
            // TODO: parse token value
            System.out.println(tokenValue);

            // TODO: search for token description
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }

    public List<Token> listTokens(String userId, byte[] accountKey) {
        throw new UnsupportedOperationException();
    }

    public void revokeToken(Token token) {
        throw new UnsupportedOperationException();
    }

    private String mapTokenToJson(Token token) {
        ObjectNode node = this.keyserver.jsonMapper.createObjectNode();
        node.put(JsonKeys.TOKEN, token.getToken());
        node.put(JsonKeys.ANNOTATION, token.getAnnotation());
        return node.toString();
    }

    private String mapTokenValueToJson(TokenValue value) {
        ObjectNode node = this.keyserver.jsonMapper.createObjectNode();
        node.put(JsonKeys.USER_ID, value.getUserId());
        node.put(JsonKeys.SERVICE_USER_ID, value.getServiceUserId());
        // roles
        ArrayNode roles = node.arrayNode();
        for (TokenValue.Role r : value.getRoles()) {
            roles.add(r.name());
        }
        node.put(JsonKeys.ROLES, roles);

        if (value instanceof InternalTokenValue) {
            InternalTokenValue iValue = (InternalTokenValue) value;
            node.put(JsonKeys.USERNAME, iValue.getUsername());
            node.put(JsonKeys.ACCOUNT_KEY, iValue.getAccountKey());
        }

        return node.toString();
    }
}
