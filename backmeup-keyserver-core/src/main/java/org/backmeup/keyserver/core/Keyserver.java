package org.backmeup.keyserver.core;

import java.util.Calendar;
import java.util.List;

import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.Token;

public interface Keyserver {

    //=========================================================================
    // User logic
    //=========================================================================
    
    String registerUser(String username, String password) throws KeyserverException;

    String registerAnonoumysUser(String username, String password) throws KeyserverException;
    
    void removeUser(String serviceUserId, String username) throws KeyserverException;
    
    void removeUser(String serviceUserId, String username, byte[] accountKey) throws KeyserverException;

    void changeUserPassword(String userId, String username, String oldPassword, String newPassword) throws KeyserverException;
    
    AuthResponse authenticateUserWithPassword(String username, String password) throws KeyserverException;
    
    void setProfile(String userId, byte[] accountKey, String profile) throws KeyserverException;
    
    String getProfile(String userId, byte[] accountKey) throws KeyserverException;
    
    String getIndexKey(String userId, byte[] accountKey) throws KeyserverException;
    
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
    
    AuthResponse createOnetime(String userId, String serviceUserId, String username, byte[] accountKey, String[] pluginIds, Calendar scheduledExecutionTime) throws KeyserverException;
    
    AuthResponse authenticateWithOnetime(String tokenHash) throws KeyserverException;

    AuthResponse authenticateWithOnetime(String tokenHash, Calendar scheduledExecutionTime) throws KeyserverException;
    
    List<Token> listTokens(String userId, byte[] accountKey, Token.Kind kind) throws KeyserverException;
    
    void revokeToken(Token.Kind kind, String tokenHash) throws KeyserverException;

    //=========================================================================
    // App logic
    //=========================================================================
    
    App registerApp(App.Approle role) throws KeyserverException;
    
    List<App> listApps(String servicePassword) throws KeyserverException;

    void removeApp(String appId) throws KeyserverException;

    App authenticateApp(String appId, String appKey) throws KeyserverException;

}