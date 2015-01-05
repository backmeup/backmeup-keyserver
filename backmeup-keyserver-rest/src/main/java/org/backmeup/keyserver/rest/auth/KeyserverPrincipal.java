package org.backmeup.keyserver.rest.auth;

import java.security.Principal;

import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.model.AuthResponse;

public class KeyserverPrincipal implements Principal {
    private final App app;
    private final AuthResponse auth;

    public KeyserverPrincipal(App app, AuthResponse auth) {
        this.app = app;
        this.auth = auth;
    }

    public App getApp() {
        return app;
    }
    
    public boolean hasTokenAuth() {
        return this.auth != null;
    }
    
    public AuthResponse getAuthResponse() {
        return auth;
    }

    @Override
    public String getName() {
        return this.app.getAppId();
    }
}
