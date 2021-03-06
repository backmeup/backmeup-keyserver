package org.backmeup.keyserver.core;

import static org.backmeup.keyserver.crypto.EncryptionUtils.fmtKey;
import static org.backmeup.keyserver.crypto.EncryptionUtils.generateSymmetricKey;
import static org.backmeup.keyserver.crypto.EncryptionUtils.hashByteArrayWithPepper;
import static org.backmeup.keyserver.model.KeyserverUtils.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.EnumUtils;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.crypto.Keyring;
import org.backmeup.keyserver.crypto.PepperApps;
import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.model.AppOrTokenRole;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.CryptoException;
import org.backmeup.keyserver.model.EntryNotFoundException;
import org.backmeup.keyserver.model.JsonKeys;
import org.backmeup.keyserver.model.KeyserverEntry;
import org.backmeup.keyserver.model.KeyserverException;
import org.backmeup.keyserver.model.Token;
import org.backmeup.keyserver.model.App.Approle;
import org.backmeup.keyserver.model.Token.Kind;
import org.backmeup.keyserver.model.TokenValue;
import org.backmeup.keyserver.model.TokenValue.Role;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

/**
 * Keyserver implementation module for token specific logic.
 * @author wolfgang
 *
 */
public class DefaultTokenLogic {
    private static final MessageFormat TK_ENTRY_FORMAT = new MessageFormat("{0}.{1}");
    private static final MessageFormat ANN_ENTRY_FORMAT = new MessageFormat("{0}."+PepperApps.ACCOUNT+".{1}.{2}");
    
    private DefaultKeyserverImpl keyserver;
    private Keyring keyring;

    public DefaultTokenLogic(DefaultKeyserverImpl keyserver) {
        this.keyserver = keyserver;
        this.keyring = this.keyserver.activeKeyring;
    }
    
    private static String tkKey(String tokenHash, String tokenKindApp) {
        return fmtKey(TK_ENTRY_FORMAT, tokenHash, tokenKindApp);
    }
    
    private static String annKey(String userId, String tokenKindApp, String tokenHash) {
        return fmtKey(ANN_ENTRY_FORMAT, userId, tokenKindApp, tokenHash);
    }
    
    public void create(Token token, byte[] accountKey) throws KeyserverException {
        this.create(token, token.getValue().getUserId(), accountKey);
    }
    
    public void create(Token token, String userId, byte[] accountKey) throws KeyserverException {
        String tokenHash = null;
        byte[] tokenKey = null;
        String tokenKindApp = token.getKind().getApplication();

        try {
            boolean collission = false;
            do {
                tokenKey = generateSymmetricKey(this.keyring);
                tokenHash = toBase64String(hashByteArrayWithPepper(this.keyring, tokenKey, tokenKindApp));

                List<KeyserverEntry> tokens = this.keyserver.db.searchByKey(tkKey(tokenHash, tokenKindApp), true, true);
                collission = !tokens.isEmpty();
            } while (collission);

            token.setToken(tokenKey);            

            // e.g. [Hash(Token)].InternalToken
            byte[] payload = this.keyserver.encryptString(tokenKey, tokenKindApp, this.mapTokenValueToJson(token.getValue()));
            this.keyserver.createEntry(tkKey(tokenHash, tokenKindApp), payload, token.getTTL());

            if (token.getAnnotation() != null && accountKey != null) {
                this.createAnnotaton(token, tokenHash, userId, accountKey);
            }
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }
    }
    
    protected void createAnnotaton(Token token, String tokenHash, String userId, byte[] accountKey) throws KeyserverException {
        try {
            String tokenKindApp = token.getKind().getApplication();
            String theHash = tokenHash;
            if (theHash == null) {
                theHash = toBase64String(hashByteArrayWithPepper(this.keyring, token.getToken(), tokenKindApp));
            }
            
            // e.g. [UserId].Account.InternalToken.[Hash(Token)]
            byte[] payload = this.keyserver.encryptString(accountKey, tokenKindApp, this.mapTokenToJson(token));
            this.keyserver.createEntry(annKey(userId, tokenKindApp, theHash), payload, token.getTTL());
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }
    }

    protected void retrieveTokenValue(Token token) throws KeyserverException {
        String tokenKindApp = token.getKind().getApplication();

        try {
            KeyserverEntry tokenEntry = this.keyserver.checkedSearchForEntry(token.getToken(), tokenKindApp, tkKey("{0}", tokenKindApp), EntryNotFoundException.TOKEN, true);
            token.setValue(this.retrieveTokenValue(token, tokenEntry));
            token.setTTL(tokenEntry.getTTL());
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
    
    private TokenValue retrieveTokenValue(Token token, KeyserverEntry tokenEntry) throws CryptoException, KeyserverException {
        String tokenValueJson = this.keyserver.decryptString(token.getToken(), token.getKind().getApplication(), tokenEntry.getValue());
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
            
            KeyserverEntry tokenAnnotationEntry = this.keyserver.checkedSearchForEntry(token.getToken(), tokenKindApp, annKey(token.getValue().getUserId(), tokenKindApp, "{0}"), EntryNotFoundException.TOKEN_ANNOTATION, true);
            Token t = retrieveTokenAnnotation(tokenAnnotationEntry, accountKey, token.getKind());
            token.setAnnotation(t.getAnnotation());
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
    
    private Token retrieveTokenAnnotation(KeyserverEntry tokenAnnotationEntry, byte[] accountKey, Kind kind) throws CryptoException, KeyserverException {
        String tokenAnnotationJson = this.keyserver.decryptString(accountKey, kind.getApplication(), tokenAnnotationEntry.getValue());
        return this.mapJsonToToken(tokenAnnotationJson, kind);
    }
    
    protected Token retrieveToken(String userId, Kind kind, String tokenHash, byte[] accountKey, boolean includeValue) throws KeyserverException {
        Token token = null;
        String tokenKindApp = kind.getApplication();
        
        try {
            KeyserverEntry tokenAnnotationEntry = this.keyserver.checkedGetEntry(annKey(userId, tokenKindApp, tokenHash), EntryNotFoundException.TOKEN_ANNOTATION, false);       
            String tokenAnnotationJson = this.keyserver.decryptString(accountKey, tokenKindApp, tokenAnnotationEntry.getValue());
            token = this.mapJsonToToken(tokenAnnotationJson, kind);
            
            if (includeValue) {
                this.retrieveTokenValue(token);
            }
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }
        
        return token;
    }

    public List<Token> listTokens(String userId, byte[] accountKey, Kind kind, boolean includeValues) throws KeyserverException {
        try {
            List<Token> tokens = new LinkedList<>();
            List<KeyserverEntry> tokenEntries = this.keyserver.db.searchByKey(annKey(userId, kind.getApplication(), "%"), false, false);
            
            for(KeyserverEntry tokenEntry : tokenEntries) {
                Token token = this.retrieveTokenAnnotation(tokenEntry, accountKey, kind);
                if(includeValues) {
                    this.retrieveTokenValue(token);
                }
                tokens.add(token);
            }
            return tokens;
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }

    public void revoke(Token token) throws KeyserverException {
        String tokenKindApp = token.getKind().getApplication();

        try {
            KeyserverEntry tokenEntry = this.keyserver.checkedSearchForEntry(token.getToken(), tokenKindApp, tkKey("{0}", tokenKindApp), EntryNotFoundException.TOKEN, false);
            
            if (!token.hasValue()) {
                token.setValue(this.retrieveTokenValue(token, tokenEntry));
            }
            this.keyserver.expireEntry(tokenEntry);
            
            KeyserverEntry tokenAnnotationEntry = this.keyserver.searchForEntry(token.getToken(), tokenKindApp, annKey(token.getValue().getUserId(), tokenKindApp, "{0}"));
            if (tokenAnnotationEntry != null) {
                this.keyserver.expireEntry(tokenAnnotationEntry);
            }  
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
    
    public void removeTokens(String userId, byte[] accountKey) throws KeyserverException {
        for (Kind kind : Kind.values()) {
            this.removeTokens(userId, accountKey, kind);
        }
    }
    
    public void removeTokens(String userId, byte[] accountKey, Kind kind) throws KeyserverException {
        List<Token> tokens = this.listTokens(userId, accountKey, kind, false);
        for (Token token : tokens) {
            this.revoke(token);
        }
    }

    private String mapTokenToJson(Token token) {
        ObjectNode node = this.keyserver.jsonMapper.createObjectNode();
        node.put(JsonKeys.TOKEN, token.getToken());
        node.put(JsonKeys.ANNOTATION, token.getAnnotation());
        return node.toString();
    }
    
    private Token mapJsonToToken(String json, Kind kind) throws KeyserverException {
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
        for (AppOrTokenRole r : value.getRoles()) {
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
            Set<AppOrTokenRole> roles = new HashSet<>();
            for (JsonNode n : rolesNode) {
                String enumValue = n.asText();
                if (EnumUtils.isValidEnum(Role.class, enumValue)) {
                    roles.add(Role.valueOf(enumValue)); 
                } else if (EnumUtils.isValidEnum(Approle.class, enumValue)) {
                    roles.add(Approle.valueOf(enumValue)); 
                } else {
                    throw new KeyserverException("role of token value no valid App.Approle or TokenValue.Role enum");
                }
                
            }
            
            value = new TokenValue(userId, serviceUserId, roles);
    
            value.setValues(this.keyserver.jsonMapper.treeToValue(tokenValueObject.get(JsonKeys.VALUES_MAP), Map.class));
        } catch (IOException e) {
            throw new KeyserverException(e);
        }
        
        return value;
    }
    
    public AuthResponse createInternal(String userId, String serviceUserId, String username, byte[] accountKey) throws KeyserverException {
        Token token = new Token(Kind.INTERNAL);
        TokenValue tokenValue = new TokenValue(userId, serviceUserId, Role.USER);
        tokenValue.putValue(JsonKeys.USERNAME, username);
        tokenValue.putValue(JsonKeys.ACCOUNT_KEY, accountKey);
        token.setValue(tokenValue);
        
        token.setTTL(getActTimePlusMinuteOffset(this.keyserver.uiTokenTimeout));
        
        this.create(token, accountKey);
        return new AuthResponse(token);
    }
    
    public AuthResponse createInternalForInheritance(String userId, String serviceUserId, byte[] accountKey, String decedentUserId, String decedentServiceUserId, byte[] decedentAccountKey) throws KeyserverException {
        Token token = new Token(Kind.INTERNAL);
        TokenValue tokenValue = new TokenValue(userId, serviceUserId, Role.INHERITANCE);
        tokenValue.putValue(JsonKeys.ACCOUNT_KEY, accountKey);
        tokenValue.putValue(JsonKeys.DECEDENT_USER_ID, decedentUserId);
        tokenValue.putValue(JsonKeys.DECEDENT_SERVICE_USER_ID, decedentServiceUserId);
        token.setValue(tokenValue);
        token.setAnnotation("Inheritance");
        
        this.create(token, decedentUserId, decedentAccountKey);
        return new AuthResponse(token);
    }
    
    public AuthResponse createInternalForAppAuthentication(String appId, App.Approle role, byte[] appKey) throws KeyserverException {
        Token token = new Token(Kind.INTERNAL);
        Set<AppOrTokenRole> roles = new HashSet<>();
        roles.add(Role.AUTHENTICATION);
        roles.add(role);
        TokenValue tokenValue = new TokenValue(appId, appId, roles);
        tokenValue.putValue(JsonKeys.ACCOUNT_KEY, appKey);
        
        token.setValue(tokenValue);
        
        this.create(token, null);
        return new AuthResponse(token);
    }
    
    public AuthResponse createOnetimeForBackup(String userId, String serviceUserId, String username, byte[] accountKey, String[] pluginIds, Calendar scheduledExecutionTime) throws KeyserverException {
        Token token = new Token(Kind.ONETIME);
        TokenValue tokenValue = new TokenValue(userId, serviceUserId, Role.BACKUP_JOB);
        tokenValue.putValue(JsonKeys.USERNAME, username);
        
        Map<String, String> pluginKeys = new HashMap<>();
        for (String pluginId : pluginIds) {
            pluginKeys.put(pluginId, toBase64String(this.keyserver.pluglinDataLogic.getPluginKey(userId, pluginId, accountKey)));
        }
        tokenValue.putValue(JsonKeys.PLUGIN_KEYS, pluginKeys);
        
        token.setValue(tokenValue);
        this.scheduleJobToken(token, scheduledExecutionTime);
        
        this.create(token, accountKey);
        return new AuthResponse(token);
    }
    
    public AuthResponse createOnetimeForAuthentication(String userId, String serviceUserId, String username, byte[] accountKey) throws KeyserverException {
        Token token = new Token(Kind.ONETIME);
        TokenValue tokenValue = new TokenValue(userId, serviceUserId, Role.AUTHENTICATION);
        tokenValue.putValue(JsonKeys.USERNAME, username);
        
        token.setValue(tokenValue);
        
        this.create(token, accountKey);
        return new AuthResponse(token);
    }
    
    private void scheduleJobToken(Token token, Calendar scheduledExecutionTime) {
        TokenValue value = token.getValue();
        
        Calendar earliestStartTime = (Calendar) scheduledExecutionTime.clone();
        earliestStartTime.add(Calendar.MINUTE, -1 * this.keyserver.backupTokenFromTimout);
        value.putValue(JsonKeys.EARLIEST_START_TIME, earliestStartTime);
        
        Calendar latestStartTime = (Calendar) scheduledExecutionTime.clone();
        latestStartTime.add(Calendar.MINUTE, this.keyserver.backupTokenToTimout);
        value.putValue(JsonKeys.LATEST_START_TIME, latestStartTime);
        
        token.setTTL(latestStartTime);
    }
    
    public AuthResponse authenticateWithInternal(String tokenHash) throws KeyserverException {
        Token token = new Token(Kind.INTERNAL, tokenHash);
        String tokenKindApp = Kind.INTERNAL.getApplication();
        
        KeyserverEntry tokenEntry;
        try {
            tokenEntry = this.keyserver.checkedSearchForEntry(token.getToken(), tokenKindApp, tkKey("{0}", tokenKindApp), EntryNotFoundException.TOKEN, true);
            
            TokenValue value = this.retrieveTokenValue(token, tokenEntry);
            token.setValue(value);
            
            Set<AppOrTokenRole> roles = value.getRoles();
            List<Approle> approles = EnumUtils.getEnumList(Approle.class);
            boolean isAppAuth = true;
            for (AppOrTokenRole r : roles) {
                if(!r.equals(Role.AUTHENTICATION) && !approles.contains(r)) {
                    isAppAuth = false;
                    break;
                }
            }
            if(!isAppAuth && !this.keyserver.userLogic.checkServiceUserId(value.getServiceUserId())) {
                this.revoke(token);
                throw new EntryNotFoundException(EntryNotFoundException.TOKEN_USER_REMOVED);
            }
            
            if (value.hasRole(Role.INHERITANCE)) {
                //"convert" to internal token
                Token internalToken = new Token(token);
                Set<AppOrTokenRole> newRoles = new HashSet<>();
                newRoles.add(Role.INHERITANCE);
                newRoles.add(Role.USER);
                internalToken.getValue().setRoles(newRoles);
                this.create(internalToken, null);
                
                token = internalToken;
            } 
            
            token.setTTL(getActTimePlusMinuteOffset(this.keyserver.uiTokenTimeout));
            this.keyserver.updateEntryTTL(tokenEntry);  
            return new AuthResponse(token);
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }
    }
    
    public AuthResponse authenticateWithOnetime(String tokenHash, boolean renew, Calendar scheduledExecutionTime) throws KeyserverException {
        Token onetimeToken = new Token(Kind.ONETIME, tokenHash);
        String tokenKindApp = Kind.ONETIME.getApplication();
        Token internalToken = null;
        AuthResponse nextAuth = null;
        
        KeyserverEntry tokenEntry;
        try {
            tokenEntry = this.keyserver.checkedSearchForEntry(onetimeToken.getToken(), tokenKindApp, tkKey("{0}", tokenKindApp), EntryNotFoundException.TOKEN, true);
            
            TokenValue value = this.retrieveTokenValue(onetimeToken, tokenEntry);
            onetimeToken.setValue(value);
            onetimeToken.setTTL(tokenEntry.getTTL());
            
            if(!this.keyserver.userLogic.checkServiceUserId(value.getServiceUserId())) {
                this.revoke(onetimeToken);
                throw new EntryNotFoundException(EntryNotFoundException.TOKEN_USER_REMOVED);
            }
            
            if(value.hasRole(Role.BACKUP_JOB) && getActTime().before(value.getValueAsCalendar(JsonKeys.EARLIEST_START_TIME))) {
                this.revoke(onetimeToken);
                throw new EntryNotFoundException(EntryNotFoundException.TOKEN_USED_TO_EARLY);
            }
            
            //"convert" to internal token
            internalToken = new Token(onetimeToken);
            internalToken.setKind(Kind.INTERNAL);
            this.create(internalToken, null);
            
            //re-schedule token
            if (renew) {
                Token newToken = new Token(onetimeToken);
                if (scheduledExecutionTime != null) {
                    this.scheduleJobToken(newToken, scheduledExecutionTime);
                }
                this.create(newToken, null);
                nextAuth = new AuthResponse(newToken);
            }
            
            this.revoke(onetimeToken);
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }
        
        return new AuthResponse(internalToken, nextAuth);
    }
    
    public AuthResponse authenticateWithExternal(String tokenHash) throws KeyserverException {
        throw new UnsupportedOperationException("not implemented yet");
        //TODO: retrieve and convert to internal token
    }
}
