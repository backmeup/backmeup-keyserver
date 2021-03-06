package org.backmeup.keyserver.model;

import java.util.Set;

/**
 * Application User will be used for applications that are allowed to connect to
 * the keyserver
 * 
 * @author spawn
 *
 */
public class App {

    public static enum Approle implements AppOrTokenRole {
        SERVICE, WORKER, STORAGE, INDEXER
    }

    private String appId;
    private String password;
    private Approle appRole;
    
    @SuppressWarnings("unused")
    private App() {
        
    }

    public App(String appId, String password, Approle appRole) {
        this.appId = appId;
        this.password = password;
        this.appRole = appRole;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Approle getAppRole() {
        return appRole;
    }

    public void setAppRole(Approle appRole) {
        this.appRole = appRole;
    }
    
    @Override
    public String toString() {
        return this.appId + " (" + this.appRole.toString() + ")";
    }
    
    public static App fromAuthResponse(AuthResponse auth) {
        Set<AppOrTokenRole> roles = auth.getRoles();
        for (AppOrTokenRole role : roles) {
            if (role instanceof App.Approle) {
                return new App(auth.getServiceUserId(), KeyserverUtils.toBase64String(auth.getAccountKey()), (App.Approle) role);
            }
        }
        throw new IllegalArgumentException("AuthResponse has no valid app role");
    }
}
