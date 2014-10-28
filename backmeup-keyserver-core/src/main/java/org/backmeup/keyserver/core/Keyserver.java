package org.backmeup.keyserver.core;

import org.backmeup.keyserver.model.AppUser;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.Token;

public interface Keyserver {

    String registerUser(String username, String password) throws KeyserverException;

    String registerAnonoumysUser(String username, String password) throws KeyserverException;

    AuthResponse authenticateUserWithPassword(String username, String password) throws KeyserverException;
    
    AuthResponse authenticateWithInternalToken(String tokenHash) throws KeyserverException;
    
    void revokeToken(Token.Kind kind, String tokenHash) throws KeyserverException;

    AppUser registerApp(AppUser.Approle role) throws KeyserverException;

    void removeApp(String appId) throws KeyserverException;

    AppUser authenticateApp(String appId, String appKey) throws KeyserverException;

}