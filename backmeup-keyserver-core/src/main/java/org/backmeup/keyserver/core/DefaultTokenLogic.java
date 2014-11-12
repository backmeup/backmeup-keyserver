package org.backmeup.keyserver.core;

import static org.backmeup.keyserver.core.KeyserverUtils.decryptString;
import static org.backmeup.keyserver.core.KeyserverUtils.encryptString;
import static org.backmeup.keyserver.core.KeyserverUtils.fmtKey;
import static org.backmeup.keyserver.core.KeyserverUtils.generateKey;
import static org.backmeup.keyserver.core.KeyserverUtils.hashByteArrayWithPepper;
import static org.backmeup.keyserver.core.KeyserverUtils.toBase64String;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;

public class DefaultTokenLogic {
    private static final MessageFormat TK_ENTRY_FORMAT = new MessageFormat("{0}.{1}");
    private static final MessageFormat ANN_ENTRY_FORMAT = new MessageFormat("{0}."+PepperApps.ACCOUNT+".{1}.{2}");
    
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
    
    private static String annKey(String userId, String tokenKindApp, String tokenHash) {
        return fmtKey(ANN_ENTRY_FORMAT, userId, tokenKindApp, tokenHash);
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

                List<KeyserverEntry> tokens = this.db.searchByKey(tkKey(tokenHash, tokenKindApp), true, true);
                collission = !tokens.isEmpty();
            } while (collission);

            token.setToken(tokenKey);            

            // e.g. [Hash(Token)].InternalToken
            KeyserverEntry ke = new KeyserverEntry(tkKey(tokenHash, tokenKindApp));
            byte[] payload = encryptString(this.keyring, hashByteArrayWithPepper(this.keyring, tokenKey, tokenKindApp), this.mapTokenValueToJson(token.getValue()));
            ke.setValue(payload);
            ke.setTTL(token.getTTL());
            this.db.putEntry(ke);

            if (token.getAnnotation() != null) {
                // e.g. [UserId].Account.InternalToken.[Hash(Token)]
                ke = new KeyserverEntry(annKey(token.getValue().getUserId(), tokenKindApp, tokenHash));
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
            KeyserverEntry tokenEntry = this.keyserver.searchForEntry(token.getToken(), tokenKindApp, tkKey("{0}", tokenKindApp));
            if (tokenEntry == null) {
                throw new EntryNotFoundException(EntryNotFoundException.TOKEN);
            }

            if (tokenEntry.getKeyringId() < this.keyring.getKeyringId()) {
                // TODO: migrate Entry
            }

            this.retrieveTokenValue(token, tokenEntry);
            token.setTTL(tokenEntry.getTTL());
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
    
    private TokenValue retrieveTokenValue(Token token, KeyserverEntry tokenEntry) throws CryptoException, KeyserverException {
        String tokenValueJson = decryptString(this.keyring, hashByteArrayWithPepper(this.keyring, token.getToken(), token.getKind().getApplication()),
                tokenEntry.getValue());
        return this.mapJsonToTokenValue(tokenValueJson);
    }

    protected void retrieveTokenAnnotation(Token token) throws KeyserverException {
        String tokenKindApp = token.getKind().getApplication();

        try {
            if (!token.hasValue()) {
                this.retrieveTokenValue(token);
            }
            
            byte[] accountKey = null;
            switch(token.getKind()) {
                case INTERNAL:            
                case EXTERNAL: 
                    accountKey = token.getValue().getValueAsByteArray(JsonKeys.ACCOUNT_KEY);
                    break;
                default: throw new IllegalArgumentException("Token of kind "+token.getKind()+" do not support an annotation");
            }
            
            KeyserverEntry tokenAnnotationEntry = this.keyserver.searchForEntry(token.getToken(), tokenKindApp, annKey(token.getValue().getUserId(), tokenKindApp, "{0}"));
            if (tokenAnnotationEntry == null) {
                throw new EntryNotFoundException(EntryNotFoundException.TOKEN_ANNOTATION);
            }

            if (tokenAnnotationEntry.getKeyringId() < this.keyring.getKeyringId()) {
                // TODO: migrate Entry
            }
          
            Token t = retrieveTokenAnnotation(tokenAnnotationEntry, accountKey, token.getKind());
            token.setAnnotation(t.getAnnotation());
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
    
    private Token retrieveTokenAnnotation(KeyserverEntry tokenAnnotationEntry, byte[] accountKey, Token.Kind kind) throws CryptoException, KeyserverException {
        String tokenAnnotationJson = decryptString(this.keyring, hashByteArrayWithPepper(this.keyring, accountKey, kind.getApplication()), tokenAnnotationEntry.getValue());
        return this.mapJsonToToken(tokenAnnotationJson, kind);
    }
    
    protected Token retrieveToken(String userId, Token.Kind kind, String tokenHash, byte[] accountKey, boolean includeValue) throws KeyserverException {
        Token token = null;
        String tokenKindApp = kind.getApplication();
        
        try {
            KeyserverEntry tokenAnnotationEntry = this.db.getEntry(annKey(userId, tokenKindApp, tokenHash));
            if (tokenAnnotationEntry == null) {
                throw new EntryNotFoundException(EntryNotFoundException.TOKEN_ANNOTATION);
            }
        
            String tokenAnnotationJson = decryptString(this.keyring, hashByteArrayWithPepper(this.keyring, accountKey, tokenKindApp), tokenAnnotationEntry.getValue());
            token = this.mapJsonToToken(tokenAnnotationJson, kind);
            if (includeValue) {
                this.retrieveTokenValue(token);
            }
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }
        
        return token;
    }

    public List<Token> listTokens(String userId, byte[] accountKey, Token.Kind kind) throws KeyserverException {
        try {
            List<Token> tokens = new LinkedList<>();
            List<KeyserverEntry> tokenEntries = db.searchByKey(annKey(userId, kind.getApplication(), "%"), false, false);
            
            for(KeyserverEntry entry : tokenEntries) {
                tokens.add(this.retrieveTokenAnnotation(entry, accountKey, kind));
            }
            return tokens;
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }

    public void revokeToken(Token token) throws KeyserverException {
        String tokenKindApp = token.getKind().getApplication();

        try {
            KeyserverEntry tokenEntry = this.keyserver.searchForEntry(token.getToken(), tokenKindApp, tkKey("{0}", tokenKindApp));
            if (tokenEntry == null) {
                throw new EntryNotFoundException(EntryNotFoundException.TOKEN);
            }

            if (!token.hasValue()) {
                token.setValue(this.retrieveTokenValue(token, tokenEntry));
            }
            tokenEntry.expire();
            this.db.updateTTL(tokenEntry);
            
            KeyserverEntry tokenAnnotationEntry = this.keyserver.searchForEntry(token.getToken(), tokenKindApp, annKey(token.getValue().getUserId(), tokenKindApp, "{0}"));
            if (tokenAnnotationEntry != null) {
                tokenAnnotationEntry.expire();
                this.db.updateTTL(tokenAnnotationEntry);
            }  
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }

    private String mapTokenToJson(Token token) {
        ObjectNode node = this.keyserver.jsonMapper.createObjectNode();
        node.put(JsonKeys.TOKEN, token.getToken());
        node.put(JsonKeys.ANNOTATION, token.getAnnotation());
        return node.toString();
    }
    
    private Token mapJsonToToken(String json, Token.Kind kind) throws KeyserverException {
        Token token = new Token(kind);

        try {
            ObjectNode tokenObject = (ObjectNode) this.keyserver.jsonMapper.readTree(json);
            
            token.setToken(tokenObject.get(JsonKeys.TOKEN).getBinaryValue());
            token.setAnnotation(tokenObject.get(JsonKeys.ANNOTATION).getTextValue());        
        } catch (IOException e) {
            throw new KeyserverException(e);
        }
        
        return token;
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

        node.put(JsonKeys.VALUES_MAP, this.keyserver.jsonMapper.valueToTree(value.getValues()));

        return node.toString();
    }
    
    @SuppressWarnings("unchecked")
    private TokenValue mapJsonToTokenValue(String json) throws KeyserverException {
        TokenValue value = null;

        try {
            ObjectNode tokenValueObject = (ObjectNode) this.keyserver.jsonMapper.readTree(json);
            
            String userId = tokenValueObject.get(JsonKeys.USER_ID).getTextValue();
            String serviceUserId = tokenValueObject.get(JsonKeys.SERVICE_USER_ID).getTextValue();
            
            // roles
            ArrayNode rolesNode = (ArrayNode) tokenValueObject.get(JsonKeys.ROLES);
            Set<TokenValue.Role> roles = new HashSet<>();
            for (JsonNode n : rolesNode) {
                roles.add(TokenValue.Role.valueOf(n.asText()));
            }
            
            value = new TokenValue(userId, serviceUserId, roles);
    
            value.setValues(this.keyserver.jsonMapper.treeToValue(tokenValueObject.get(JsonKeys.VALUES_MAP), Map.class));
        } catch (IOException e) {
            throw new KeyserverException(e);
        }
        
        return value;
    }
    
    public AuthResponse authenticateWithInternalToken(String tokenHash) throws KeyserverException {
        Token token = new Token(Token.Kind.INTERNAL, tokenHash);
        String tokenKindApp = Token.Kind.INTERNAL.getApplication();
        
        KeyserverEntry tokenEntry;
        try {
            tokenEntry = this.keyserver.searchForEntry(token.getToken(), tokenKindApp, tkKey("{0}", tokenKindApp));
            if (tokenEntry == null) {
                throw new EntryNotFoundException(EntryNotFoundException.TOKEN);
            }
    
            if (tokenEntry.getKeyringId() < this.keyring.getKeyringId()) {
                // TODO: migrate Entry
            }
            
            TokenValue value = this.retrieveTokenValue(token, tokenEntry);
            if(!this.keyserver.userLogic.checkServiceUserId(value.getServiceUserId())) {
                throw new EntryNotFoundException(EntryNotFoundException.TOKEN_USER_REMOVED);
            }
            token.setValue(value);
                    
            Calendar ttl = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            ttl.add(Calendar.MINUTE, this.keyserver.uiTokenTimeout);
            token.setTTL(ttl);
            tokenEntry.setTTL(ttl);
            this.db.updateTTL(tokenEntry);
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }
        
        return new AuthResponse(token);
    }
    
    public AuthResponse authenticateWithOnetimeToken(String tokenHash) throws KeyserverException {
        throw new UnsupportedOperationException("not implemented yet");
        //TODO: retrieve and convert to internal token (change response type!)
    }
    
    public AuthResponse authenticateWithExternalToken(String tokenHash) throws KeyserverException {
        throw new UnsupportedOperationException("not implemented yet");
        //TODO: retrieve and convert to internal token
    }
}
