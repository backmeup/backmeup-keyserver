package org.backmeup.keyserver.core;

import java.util.List;

import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.Token;

public interface Keyserver {

    String registerUser(String username, String password) throws KeyserverException;

    String registerAnonoumysUser(String username, String password) throws KeyserverException;

    AuthResponse authenticateUserWithPassword(String username, String password) throws KeyserverException;
    
    AuthResponse authenticateWithInternalToken(String tokenHash) throws KeyserverException;
    
    List<Token> listTokens(String userId, byte[] accountKey, Token.Kind kind) throws KeyserverException;
    
    void revokeToken(Token.Kind kind, String tokenHash) throws KeyserverException;

    App registerApp(App.Approle role) throws KeyserverException;
    
    List<App> listApps(String servicePassword) throws KeyserverException;

    void removeApp(String appId) throws KeyserverException;

    App authenticateApp(String appId, String appKey) throws KeyserverException;

}