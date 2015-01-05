package org.backmeup.keyserver.rest.auth;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.Token;

public class KeyserverSecurityContext implements SecurityContext {
    private final KeyserverPrincipal principal;

    public KeyserverSecurityContext(App app, AuthResponse auth) {
        this.principal = new KeyserverPrincipal(app, auth);
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        return principal.getApp().getAppRole().toString().equals(role);
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return null;
    }

}
