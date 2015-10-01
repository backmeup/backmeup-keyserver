package org.backmeup.keyserver.core;

import java.util.Calendar;
import java.util.List;

import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.KeyserverException;
import org.backmeup.keyserver.model.Token;

/**
 * Public interface a Keyserver has to provide.
 * @author wolfgang
 *
 */
public interface Keyserver {

    //=========================================================================
    // User logic
    //=========================================================================
    
    String registerUser(String username, String password) throws KeyserverException;

    AuthResponse registerAnonymousUser(String decedantServiceUserId, String decedantUserId, byte[] decedantAccountKey) throws KeyserverException;
    
    void removeAnonymousUser(String serviceUserId, String userId, byte[] accountKey) throws KeyserverException;
    
    void removeUser(String serviceUserId, String username) throws KeyserverException;
    
    void removeUser(String serviceUserId, String username, byte[] accountKey) throws KeyserverException;

    void changeUserPassword(String userId, String username, String oldPassword, String newPassword) throws KeyserverException;
    
    AuthResponse authenticateUserWithPassword(String username, String password) throws KeyserverException;
    
    void setProfile(String userId, byte[] accountKey, String profile) throws KeyserverException;
    
    String getProfile(String userId, byte[] accountKey) throws KeyserverException;
    
    String getIndexKey(String userId, byte[] accountKey) throws KeyserverException;
    
    void setIndexKey(String userId, byte[] accountKey, String indexKey) throws KeyserverException;
    
    byte[] getPublicKey(String userId) throws KeyserverException;
    
    byte[] getPublicKeyByUsername(String username) throws KeyserverException;
    
    byte[] getPrivateKey(String userId, byte[] accountKey) throws KeyserverException;
    
  //=========================================================================
    // PluginData logic
    //=========================================================================
    
    void createPluginData(String userId, String pluginId, byte[] accountKey, String data) throws KeyserverException;
    
    byte[] getPluginDataKey(String userId, String pluginId, byte[] accountKey) throws KeyserverException;
    
    void removePluginData(String userId, String pluginId) throws KeyserverException;
    
    void updatePluginData(String userId, String pluginId, byte[] pluginKey, String data) throws KeyserverException;
    
    String getPluginData(String userId, String pluginId, byte[] pluginKey) throws KeyserverException;
    
    //=========================================================================
    // Token logic
    //=========================================================================
    
    AuthResponse authenticateWithInternalToken(String tokenHash) throws KeyserverException;
    
    AuthResponse createOnetimeForBackup(String userId, String serviceUserId, String username, byte[] accountKey, String[] pluginIds, Calendar scheduledExecutionTime) throws KeyserverException;
    
    AuthResponse createOnetimeForAuthentication(String userId, String serviceUserId, String username, byte[] accountKey) throws KeyserverException;

    AuthResponse authenticateWithOnetime(String tokenHash, boolean renew, Calendar scheduledExecutionTime) throws KeyserverException;
        
    List<Token> listTokens(String userId, byte[] accountKey, Token.Kind kind, boolean includeValues) throws KeyserverException;
    
    void revokeToken(Token.Kind kind, String tokenHash) throws KeyserverException;

    //=========================================================================
    // App logic
    //=========================================================================
    
    App registerApp(App.Approle role) throws KeyserverException;
    
    List<App> listApps(String servicePassword) throws KeyserverException;

    void removeApp(String appId) throws KeyserverException;

    AuthResponse authenticateApp(String appId, String appKey) throws KeyserverException;
}